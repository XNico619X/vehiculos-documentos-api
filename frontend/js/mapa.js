// ============================================================================
// mapa.js · UTILIDADES DE GOOGLE MAPS (no es una pantalla: la pantalla es vistas/mapa.js)
// ============================================================================
// Mismo funcionamiento que antes, con estos cambios para la nueva estructura:
//   · destruirMapa(): al salir de la pantalla del mapa se suelta todo. Al volver, se crea un mapa
//     NUEVO en el <div> nuevo (un mapa de Google queda atado al <div> donde nació; si el router
//     reemplaza ese <div>, el mapa viejo ya no sirve).
//   · El script de Google se descarga UNA sola vez por sesión del navegador.
//   · Colores de la nueva paleta: la línea de la ruta es lima; los pines se estilizan en vistas.css.

import { CONFIG } from "../config.js";

let mapa = null;
let clases = null;                // clases de Google ya cargadas (InfoWindow, Polyline, ...)
let ventana = null;               // la burbuja de información que se abre al tocar un marcador
let linea = null;                 // la línea que une las paradas en orden
const marcadores = new Map();     // índice de parada -> { marcador, parada, posicion }
let promesaScript = null;         // la descarga del script de Google (para no repetirla)

// Una parada "tiene coordenadas" solo si latitud y longitud son números reales (null = la tarea programada aún no las completa)
export function tieneCoordenadas(parada) {
    const valido = (v) => v !== null && v !== undefined && v !== "" && Number.isFinite(Number(v));
    return valido(parada.latitud) && valido(parada.longitud);
}

// 0 = inicial, la última = final, el resto = intermedias
export function tipoDeParada(indice, total) {
    if (indice === 0) return "inicio";
    if (indice === total - 1) return "fin";
    return "intermedia";
}

// Carga el script de Google con la clave de config.js. Devuelve una promesa que se cumple cuando Google termina de cargar.
function cargarScriptGoogle() {
    if (window.google && window.google.maps) return Promise.resolve();
    if (promesaScript) return promesaScript;
    promesaScript = new Promise((resolver, rechazar) => {
        window.__mapsListo = () => resolver(); // Google llama a esta función al terminar (parámetro callback=)
        const script = document.createElement("script");
        script.src = "https://maps.googleapis.com/maps/api/js?key=" + encodeURIComponent(CONFIG.MAPS_API_KEY)
            + "&callback=__mapsListo&loading=async&v=weekly";
        script.async = true;
        script.onerror = () => {
            script.remove();
            promesaScript = null; // permite reintentar
            rechazar(new Error("No se pudo cargar Google Maps. Revisa la conexión y MAPS_API_KEY en config.js."));
        };
        document.head.appendChild(script);
    });
    return promesaScript;
}

// Crea el mapa dentro de `elemento`. `alError` recibe un texto si Google rechaza la clave.
export async function iniciarMapa(elemento, alError) {
    // Google llama a esta función global cuando rechaza la clave (restricción de sitio, API no habilitada...)
    window.gm_authFailure = () => alError?.(
        `Google Maps rechazó la clave: revisa que la Maps JavaScript API esté habilitada y que ${window.location.origin} esté permitido en la clave.`);

    await cargarScriptGoogle();
    const { Map: MapaGoogle, InfoWindow, Polyline } = await google.maps.importLibrary("maps");
    const { LatLngBounds } = await google.maps.importLibrary("core");
    const { AdvancedMarkerElement } = await google.maps.importLibrary("marker");
    clases = { InfoWindow, Polyline, LatLngBounds, AdvancedMarkerElement };

    mapa = new MapaGoogle(elemento, {
        center: { lat: 4.4389, lng: -75.2322 }, // Ibagué
        zoom: 13,
        mapId: "DEMO_MAP_ID",                   // los marcadores avanzados exigen un mapId; este valor sirve para desarrollo
        colorScheme: "DARK",                    // mapa oscuro para combinar con la interfaz (si el mapId no lo admite, se ignora y se ve claro)
        streetViewControl: false,
        mapTypeControl: false,
    });
    ventana = new InfoWindow();
}

// Suelta el mapa actual (se llama al salir de la pantalla)
export function destruirMapa() {
    limpiarMapa();
    mapa = null;
    ventana = null;
}

export const hayMapa = () => mapa !== null;

// Dibuja los marcadores numerados y la línea que los une. Devuelve cuántas paradas pudo poner en el mapa.
export function dibujarRuta(paradas, { encuadrar = true } = {}) {
    limpiarMapa();
    const puntos = [];
    const limites = new clases.LatLngBounds();

    paradas.forEach((parada, indice) => {
        if (!tieneCoordenadas(parada)) return; // las pendientes se ven en la lista, no en el mapa

        // latitud es latitud y longitud es longitud. NO se intercambian.
        const posicion = { lat: Number(parada.latitud), lng: Number(parada.longitud) };

        // El marcador es un div nuestro (estilo .pin en vistas.css) con el número de la parada
        const pin = document.createElement("div");
        pin.className = "pin " + tipoDeParada(indice, paradas.length);
        pin.textContent = String(indice + 1);

        const marcador = new clases.AdvancedMarkerElement({ map: mapa, position: posicion, content: pin, title: parada.ubicacion });
        marcador.addListener("click", () => abrirVentana(marcador, parada, indice));

        marcadores.set(indice, { marcador, parada, posicion });
        puntos.push(posicion);
        limites.extend(posicion);
    });

    if (puntos.length >= 2) {
        linea = new clases.Polyline({ path: puntos, map: mapa, strokeColor: "#d4ff24", strokeOpacity: 0.9, strokeWeight: 4 });
    }
    if (encuadrar) encuadrarMapa(puntos, limites);
    return puntos.length;
}

// Se llama al tocar una parada de la lista lateral. Devuelve false si esa parada aún no tiene coordenadas.
export function enfocarParada(indice) {
    const registro = marcadores.get(indice);
    if (!registro) return false;
    mapa.panTo(registro.posicion);
    abrirVentana(registro.marcador, registro.parada, indice);
    return true;
}

export function limpiarMapa() {
    marcadores.forEach(({ marcador }) => { marcador.map = null; }); // quitar un marcador avanzado = ponerle map en null
    marcadores.clear();
    if (linea) { linea.setMap(null); linea = null; }
    if (ventana) ventana.close();
}

function encuadrarMapa(puntos, limites) {
    if (puntos.length === 0) return;
    if (puntos.length === 1) { mapa.setCenter(puntos[0]); mapa.setZoom(15); return; }
    mapa.fitBounds(limites, 60); // ajusta zoom y centro para que se vean todas las paradas, con 60 px de margen
}

function abrirVentana(marcador, parada, indice) {
    // Se arma con textContent y NO con innerHTML: la ubicación viene de la base de datos y no debe interpretarse como HTML.
    // (La burbuja de Google tiene fondo blanco, por eso el texto se fuerza a negro.)
    const caja = document.createElement("div");
    caja.style.color = "#111";
    const titulo = document.createElement("strong");
    titulo.textContent = `Parada ${indice + 1}`;
    const texto = document.createElement("p");
    texto.textContent = parada.ubicacion;
    caja.append(titulo, texto);
    ventana.setContent(caja);
    ventana.open({ anchor: marcador, map: mapa });
}

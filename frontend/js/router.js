// ============================================================================
// router.js · NAVEGACIÓN POR HASH  (#/publico, #/vehiculos, #/mapa/RUTA-001 ...)
// ============================================================================
// ¿Por qué hash (#) y no rutas "normales" (/vehiculos)?
//   Con hash el navegador NO le pide nada al servidor al cambiar de pantalla, así que
//   funciona con cualquier servidor estático (Live Server, etc.) sin configurar nada.
//   Y como cada cambio de hash es una entrada del historial, el botón "Atrás" funciona.
//
// Cómo se agrega una pantalla nueva (partes 1 a 6): una línea en RUTAS + un archivo en js/vistas/.
// Cada vista exporta:   export async function montar(contenedor, ctx) { ...; return función_de_limpieza; }
//   · contenedor: el <div> donde dibujar la vista (propio de esta navegación)
//   · ctx: { ruta, params, consulta, navegar }
//   · lo que devuelva (opcional) es una función que se llama al SALIR de la vista
//     (para cancelar temporizadores, destruir el mapa, etc.)

import { obtenerSesion } from "./session.js";
import { el } from "./ui/util.js";
import { estadoCarga, estadoError } from "./ui/mensajes.js";

// Adónde se va tras iniciar sesión si el usuario no venía de una página concreta
export const DESTINO_TRAS_LOGIN = "/publico";
const RUTA_INICIO = "/publico";

// ----------------------------------------------------------------------------
// TABLA DE RUTAS. Campos:
//   ruta        patrón del hash; ":nombre" captura un trozo y lo entrega en ctx.params
//   titulo      título de la pestaña y anuncio para lectores de pantalla
//   etiqueta    texto del menú (si menu: true)
//   menu        ¿aparece en el menú principal?
//   protegida   true = exige sesión; sin ella se redirige al login
//   soloInvitado true = solo para quien NO tiene sesión (el login)
//   parte, titular, descripcion   los usa la vista "próximamente" mientras la pantalla se construye
//   cargar      import() dinámico: el JS de una pantalla se descarga solo cuando se visita
// ----------------------------------------------------------------------------
const proximamente = () => import("./vistas/proximamente.js");

export const RUTAS = [
    // ---- Públicas ----
    { ruta: "/publico", titulo: "Inicio", etiqueta: "Inicio", menu: true, protegida: false,
        cargar: () => import("./vistas/publico.js") },
    { ruta: "/login", titulo: "Iniciar sesión", protegida: false, soloInvitado: true,
        cargar: () => import("./vistas/login.js") },

    // ---- Protegidas (necesitan sesión) ----
    { ruta: "/vehiculos", titulo: "Vehículos", etiqueta: "Vehículos", menu: true, protegida: true, cargar: proximamente,
        parte: 3, titular: ["Flota de ", "vehículos"],
        descripcion: "Lista, búsqueda, creación y edición de vehículos, con el estado de sus documentos." },
    { ruta: "/documentos", titulo: "Documentos", etiqueta: "Documentos", menu: true, protegida: true, cargar: proximamente,
        parte: 3, titular: ["Catálogo de ", "documentos"],
        descripcion: "Crear, editar y eliminar los tipos de documento que se exigen a los vehículos." },
    { ruta: "/personas", titulo: "Personas", etiqueta: "Personas", menu: true, protegida: true, cargar: proximamente,
        parte: 4, titular: ["Registro de ", "personas"],
        descripcion: "Conductores y administrativos: crear, editar y cargar la licencia." },
    { ruta: "/conductores", titulo: "Conductores", etiqueta: "Conductores", menu: true, protegida: true, cargar: proximamente,
        parte: 4, titular: ["Conductores por ", "vehículo"],
        descripcion: "Asociar conductores a un vehículo y cambiar su estado (PO, EA, RO)." },
    { ruta: "/rutas", titulo: "Rutas", etiqueta: "Rutas", menu: true, protegida: true, cargar: proximamente,
        parte: 5, titular: ["Rutas y ", "trayectos"],
        descripcion: "Crear una ruta con sus paradas y consultarla por conductor o por placa." },
    { ruta: "/cargues", titulo: "Cargue de Excel", etiqueta: "Cargues", menu: true, protegida: true,
        cargar: () => import("./vistas/cargues.js") },
    // Resultado de un cargue concreto: al terminar de subir se navega aquí, y así "Atrás" y F5 funcionan
    { ruta: "/cargues/:idCargue", titulo: "Resultado del cargue", protegida: true,
        cargar: () => import("./vistas/cargues.js") },
    { ruta: "/cuenta", titulo: "Mi cuenta", protegida: true, cargar: proximamente,
        parte: 2, titular: ["Mi ", "cuenta"],
        descripcion: "Cambiar la contraseña y regenerar la API Key." },

    // ---- El mapa de ruta que ya existía (ahora es una pantalla más) ----
    { ruta: "/mapa", titulo: "Mapa de ruta", etiqueta: "Mapa", menu: true, protegida: true,
        cargar: () => import("./vistas/mapa.js") },
    { ruta: "/mapa/:codigo", titulo: "Mapa de ruta", protegida: true,
        cargar: () => import("./vistas/mapa.js") },
];

// Convierte "/mapa/:codigo" en una expresión regular y recuerda los nombres de los parámetros
const COMPILADAS = RUTAS.map((r) => {
    const nombres = [];
    const patron = r.ruta.replace(/:([a-zA-Z]+)/g, (_, nombre) => { nombres.push(nombre); return "([^/]+)"; });
    return { definicion: r, regex: new RegExp(`^${patron}$`), nombres };
});

// ------------------------------ Estado del router ---------------------------
let contenedorPrincipal = null;
let alCambiarRuta = null;
let limpiezaActual = null;  // función que devolvió la vista anterior
let rutaVigente = null;     // definición de la ruta que se está mostrando
let numeroNavegacion = 0;   // sirve para ignorar una vista que terminó de cargar DESPUÉS de que el usuario ya navegó a otra

export const rutaActual = () => rutaVigente;

// Lee el hash actual: "#/mapa/R1?x=2"  ->  { camino: "/mapa/R1", consulta: URLSearchParams }
function leerHash() {
    const crudo = window.location.hash.slice(1);        // quita el "#"
    const [camino, texto = ""] = crudo.split("?");
    return { camino: camino.startsWith("/") ? camino : "", consulta: new URLSearchParams(texto) };
}

function buscarRuta(camino) {
    for (const { definicion, regex, nombres } of COMPILADAS) {
        const coincidencia = regex.exec(camino);
        if (!coincidencia) continue;
        const params = {};
        nombres.forEach((nombre, i) => { params[nombre] = decodeURIComponent(coincidencia[i + 1]); });
        return { definicion, params };
    }
    return null;
}

// Cambia de pantalla. reemplazar = true sustituye la entrada actual del historial (no deja
// rastro): se usa en redirecciones, para que "Atrás" no devuelva a una página que ya redirigió.
export function navegar(destino, { reemplazar = false } = {}) {
    const nuevo = `#${destino}`;
    if (window.location.hash === nuevo) { resolver(); return; } // mismo hash: no habría evento, se resuelve a mano
    if (reemplazar) window.location.replace(nuevo);
    else window.location.hash = nuevo;
}

// Lleva al login recordando a dónde quería ir el usuario: #/login?volver=/vehiculos
export function irALogin(volverA = "") {
    const sufijo = volverA && volverA !== "/login" ? `?volver=${encodeURIComponent(volverA)}` : "";
    navegar(`/login${sufijo}`, { reemplazar: true });
}

export function iniciarRouter({ contenedor, alCambiar }) {
    contenedorPrincipal = contenedor;
    alCambiarRuta = alCambiar;
    window.addEventListener("hashchange", () => resolver());
    resolver(true);
}

// ----------------------------------------------------------------------------
// El corazón: decide qué pantalla mostrar para el hash actual
// ----------------------------------------------------------------------------
async function resolver(primeraVez = false) {
    const { camino, consulta } = leerHash();
    const encontrada = buscarRuta(camino);

    // Hash vacío o desconocido -> pantalla de inicio (o aviso de "no encontrada" si escribieron algo raro)
    if (!encontrada) {
        if (camino === "" || camino === "/") { navegar(RUTA_INICIO, { reemplazar: true }); return; }
        mostrarNoEncontrada(camino);
        return;
    }

    const { definicion, params } = encontrada;

    // GUARDIA 1: pantalla protegida sin sesión vigente -> al login (y se recuerda a dónde iba)
    if (definicion.protegida && !obtenerSesion()) { irALogin(camino); return; }
    // GUARDIA 2: el login no tiene sentido si ya hay sesión
    if (definicion.soloInvitado && obtenerSesion()) { navegar(DESTINO_TRAS_LOGIN, { reemplazar: true }); return; }

    const miNumero = ++numeroNavegacion;

    // 1) Limpiar la vista anterior (temporizadores, mapa...)
    try { limpiezaActual?.(); } catch (error) { console.error("Error al limpiar la vista anterior:", error); }
    limpiezaActual = null;
    rutaVigente = definicion;

    // 2) Cada navegación recibe SU PROPIO <div>. Así, si una vista lenta termina de cargar cuando el
    //    usuario ya está en otra pantalla, escribe en un <div> descartado y no ensucia la pantalla actual.
    const vista = el("div", { class: "vista" }, estadoCarga());
    contenedorPrincipal.replaceChildren(vista);

    const ctx = { ruta: definicion, params, consulta, navegar };
    alCambiarRuta?.(definicion, ctx, primeraVez); // título, menú activo, foco, anuncio...

    // 3) Cargar el código de la vista (solo se descarga la primera vez) y dibujarla
    try {
        const modulo = await definicion.cargar();
        if (miNumero !== numeroNavegacion) return; // el usuario ya se fue a otra pantalla
        vista.replaceChildren();
        const limpieza = await modulo.montar(vista, ctx);
        if (miNumero !== numeroNavegacion) { if (typeof limpieza === "function") limpieza(); return; }
        limpiezaActual = typeof limpieza === "function" ? limpieza : null;
    } catch (error) {
        console.error(`Error al abrir ${definicion.ruta}:`, error);
        if (miNumero === numeroNavegacion) {
            vista.replaceChildren(estadoError("No se pudo abrir esta pantalla. Recarga la página e intenta de nuevo."));
        }
    }
}

function mostrarNoEncontrada(camino) {
    try { limpiezaActual?.(); } catch { /* nada */ }
    limpiezaActual = null;
    rutaVigente = null;
    numeroNavegacion++;
    contenedorPrincipal.replaceChildren(el("div", { class: "vista" },
        el("div", { class: "estado" },
            el("h2", {}, "Página ", el("em", {}, "no encontrada")),
            el("p", {}, `No existe la dirección “${camino}”.`),
            el("a", { class: "boton boton--primario boton--chico", href: `#${RUTA_INICIO}` }, "Ir al inicio"))));
    alCambiarRuta?.({ ruta: camino, titulo: "No encontrada" }, { params: {}, consulta: new URLSearchParams() }, false);
}

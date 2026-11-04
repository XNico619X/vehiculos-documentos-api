// ============================================================================
// vistas/consultas/consultasPublicas.js · LAS 6 CONSULTAS PÚBLICAS
// ============================================================================
// Dibuja 6 tarjetas (cada una es un pequeño formulario con su botón «Consultar») y, debajo,
// una zona de RESULTADO que se rellena con la consulta que el usuario pulse.
//
// Todas llaman a rutas /api/publico/** del backend, que NO piden token ni APIKey (apiPublico).
//
// Para AGREGAR una consulta nueva: se agrega un objeto a la lista CONSULTAS de abajo. Cada una define:
//   id, titulo, descripcion      textos de la tarjeta
//   campo (opcional)             el dato que pide (días, placa); si no pide nada, null
//   ruta(valores)                la dirección del backend a la que se llama
//   esVacio(datos)               ¿la respuesta no trae nada que mostrar?
//   vacio                        texto cuando no hay datos ("No hay vehículos con documentos vencidos")
//   pintar(datos, valores)       devuelve el nodo con el resultado (funciones de resultados.js)
//   resumen(datos)               frase corta para el lector de pantalla ("3 vehículos encontrados")

import { apiPublico } from "../../api.js";
import { crearCampo, enviarFormulario, datosFormulario } from "../../ui/formulario.js";
import { estadoCarga, estadoVacio, estadoError, anunciar } from "../../ui/mensajes.js";
import { el, boton } from "../../ui/util.js";
import {
    tablaVehiculosConDocumentos, resaltarVencidos, resaltarPorVencer, tablaConductores,
    fichaVehiculoCompleto, tarjetasConteo, totalConteo, listaRutasRestringidas,
} from "./resultados.js";

// "1 vehículo" / "3 vehículos"
const plural = (n, uno, varios) => `${n} ${n === 1 ? uno : varios}`;

const CONSULTAS = [
    {
        id: "vencidos",
        titulo: "Documentos vencidos",
        descripcion: "Vehículos que tienen al menos un documento vencido.",
        campo: null,
        ruta: () => "/api/publico/vehiculos/documentos-vencidos",
        esVacio: (d) => d.length === 0,
        vacio: { titulo: "No hay vehículos con documentos vencidos", texto: "Todos los documentos están al día." },
        pintar: (d) => tablaVehiculosConDocumentos(d, resaltarVencidos, "Vehículos con documentos vencidos"),
        resumen: (d) => `${plural(d.length, "vehículo encontrado", "vehículos encontrados")}`,
    },
    {
        id: "por-vencer",
        titulo: "Documentos por vencer",
        descripcion: "Vehículos con documentos que vencen dentro de los próximos días que indiques.",
        campo: {
            nombre: "dias", etiqueta: "Número de días", tipo: "number", valor: "30", requerido: true,
            ayuda: "Desde hoy hasta ese número de días.",
            atributos: { min: 1, max: 3650, step: 1, inputmode: "numeric" },
        },
        ruta: (v) => `/api/publico/vehiculos/documentos-por-vencer?dias=${encodeURIComponent(v.dias)}`,
        esVacio: (d) => d.length === 0,
        vacio: (v) => ({ titulo: `No hay documentos por vencer en ${v.dias} días`, texto: "Prueba con un número de días mayor." }),
        pintar: (d, v) => tablaVehiculosConDocumentos(d, resaltarPorVencer(Number(v.dias)), `Vehículos con documentos que vencen en ${v.dias} días`),
        resumen: (d) => `${plural(d.length, "vehículo encontrado", "vehículos encontrados")}`,
    },
    {
        id: "conductores",
        titulo: "Conductores disponibles",
        descripcion: "Conductores que pueden operar en este momento (estado PO).",
        campo: null,
        ruta: () => "/api/publico/conductores-disponibles",
        esVacio: (d) => d.length === 0,
        vacio: { titulo: "No hay conductores disponibles", texto: "Ningún conductor está en estado «Puede operar»." },
        pintar: (d) => tablaConductores(d),
        resumen: (d) => `${plural(d.length, "conductor disponible", "conductores disponibles")}`,
    },
    {
        id: "placa",
        titulo: "Vehículo por placa",
        descripcion: "Datos del vehículo con sus documentos y los conductores asociados.",
        campo: {
            nombre: "placa", etiqueta: "Placa", tipo: "text", requerido: true, placeholder: "ABC123",
            ayuda: "Automóvil: ABC123 · Motocicleta: ABC12D",
            atributos: {
                maxlength: 6, autocomplete: "off", spellcheck: "false", autocapitalize: "characters",
                // Si no cumple el patrón, formulario.js muestra este texto como error
                pattern: "[A-Za-z]{3}[0-9]{2}[0-9A-Za-z]", title: "La placa debe tener 6 caracteres: ABC123 (automóvil) o ABC12D (motocicleta).",
            },
        },
        // La placa se guarda en mayúsculas en la base de datos
        ruta: (v) => `/api/publico/vehiculos/placa/${encodeURIComponent(v.placa.toUpperCase())}`,
        esVacio: () => false,   // un 404 lo maneja el catch de ejecutar()
        pintar: (d) => fichaVehiculoCompleto(d),
        resumen: (d) => `Vehículo ${d.placa} encontrado`,
    },
    {
        id: "personas",
        titulo: "Personas por tipo",
        descripcion: "Cuántos conductores y cuántos administrativos hay registrados.",
        campo: null,
        ruta: () => "/api/publico/personas/conteo-por-tipo",
        esVacio: (d) => d.length === 0,
        vacio: { titulo: "Aún no hay personas registradas", texto: "Cuando se registren aparecerán aquí." },
        pintar: (d) => tarjetasConteo(d),
        resumen: (d) => `${plural(totalConteo(d), "persona registrada", "personas registradas")}`,
    },
    {
        id: "restringidas",
        titulo: "Rutas restringidas",
        descripcion: "Rutas cuyo vehículo no está habilitado o cuyo conductor está restringido.",
        campo: null,
        ruta: () => "/api/publico/trayectos/rutas-restringidas",
        esVacio: (d) => d.length === 0,
        vacio: { titulo: "No hay rutas restringidas", texto: "Todas las rutas cumplen las condiciones para operar." },
        pintar: (d) => listaRutasRestringidas(d),
        resumen: (d) => `${plural(d.length, "ruta restringida", "rutas restringidas")}`,
    },
];

// Mueve la pantalla hasta el resultado (sin animación si el usuario pidió "reducir movimiento")
function desplazarA(elemento) {
    const reducir = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    elemento.scrollIntoView({ behavior: reducir ? "auto" : "smooth", block: "start" });
}

// Construye toda la sección dentro de `contenedor`. Devuelve una función de limpieza.
export function montarConsultas(contenedor) {
    let consecutivo = 0; // numera las consultas: si el usuario pulsa otra antes de que llegue la respuesta, la vieja se ignora
    let tarjetaActiva = null;

    // ---- Zona de resultado (empieza oculta) ----
    // tabindex="-1": el título recibe el foco por programa al terminar la consulta, y el lector de pantalla lo lee.
    const tituloResultado = el("h3", { id: "resultado-titulo", tabindex: "-1" });
    const cuerpoResultado = el("div", { class: "resultado-cuerpo" });
    const zonaResultado = el("section", { class: "resultado", "aria-labelledby": "resultado-titulo", hidden: true },
        el("div", { class: "resultado-cabecera" },
            tituloResultado,
            boton("Cerrar resultado", {
                chico: true,
                onclick: () => {
                    consecutivo++;                       // ignora respuestas pendientes
                    zonaResultado.hidden = true;
                    tarjetaActiva?.classList.remove("consulta--activa");
                    tarjetaActiva?.querySelector("button[type=submit]")?.focus(); // el foco vuelve al botón que la abrió
                },
            })),
        cuerpoResultado);

    // ---- Ejecutar una consulta ----
    async function ejecutar(consulta, valores, tarjeta) {
        const mio = ++consecutivo;
        tarjetaActiva?.classList.remove("consulta--activa");
        tarjetaActiva = tarjeta;
        tarjeta.classList.add("consulta--activa");

        tituloResultado.textContent = `Resultado · ${consulta.titulo}`;
        cuerpoResultado.replaceChildren(estadoCarga("Consultando…"));
        const eraVisible = !zonaResultado.hidden;
        zonaResultado.hidden = false;
        if (!eraVisible) desplazarA(zonaResultado);

        try {
            const datos = await apiPublico(consulta.ruta(valores));
            if (mio !== consecutivo) return; // el usuario ya pidió otra cosa

            if (consulta.esVacio(datos)) {
                const vacio = typeof consulta.vacio === "function" ? consulta.vacio(valores) : consulta.vacio;
                cuerpoResultado.replaceChildren(estadoVacio(vacio.titulo, vacio.texto));
                anunciar(vacio.titulo);
            } else {
                cuerpoResultado.replaceChildren(consulta.pintar(datos, valores));
                anunciar(consulta.resumen(datos));
            }
        } catch (error) {
            if (mio !== consecutivo) return;
            // 404 = "no existe" (ej. una placa que no está registrada): no es una falla, es una respuesta
            if (error.estado === 404) {
                cuerpoResultado.replaceChildren(estadoVacio("No se encontró", error.message));
                anunciar(error.message);
            } else {
                cuerpoResultado.replaceChildren(
                    estadoError(error.message, () => ejecutar(consulta, valores, tarjeta)));
            }
        }
        if (mio === consecutivo) tituloResultado.focus({ preventScroll: true });
    }

    // ---- Una tarjeta por consulta ----
    const tarjetas = CONSULTAS.map((consulta, i) => {
        const campo = consulta.campo ? crearCampo(consulta.campo) : null;
        const titulo = `consulta-${consulta.id}-titulo`;

        const formulario = el("form", { class: "consulta tarjeta tarjeta--interactiva", "aria-labelledby": titulo },
            el("span", { class: "consulta-numero", "aria-hidden": "true" }, String(i + 1).padStart(2, "0")),
            el("h3", { id: titulo }, consulta.titulo),
            el("p", { class: "consulta-descripcion" }, consulta.descripcion),
            campo?.contenedor,
            el("div", { class: "formulario-acciones" },
                boton("Consultar", { variante: "primario", tipo: "submit" })));

        enviarFormulario(formulario, {
            textoCargando: "Consultando…",
            alEnviar: (form) => ejecutar(consulta, datosFormulario(form), formulario),
        });
        return formulario;
    });

    contenedor.append(el("div", { class: "consultas-rejilla" }, tarjetas), zonaResultado);

    return () => { consecutivo++; }; // al salir de la pantalla, cualquier respuesta que llegue tarde se descarta
}
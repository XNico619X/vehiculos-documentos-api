// ============================================================================
// vistas/mapa.js · PANTALLA "MAPA DE RUTA"  (#/mapa  y  #/mapa/CODIGO)
// ============================================================================
// Es la pantalla que antes era "vista-ruta" del index.html, con la misma lógica:
//   · el usuario escribe un código de ruta  ->  GET /api/trayectos/rutas/{codigo}
//   · se dibujan las paradas (lista lateral + marcadores + línea) en el mapa de Google
//   · si faltan coordenadas, se reconsulta sola cada CONFIG.REFRESCO_MS (la tarea programada
//     del backend las completa cada ~90 s), con un tope de MAX_REFRESCOS intentos
//
// Novedades:
//   · si la URL trae el código (#/mapa/PRUEBA-001) la ruta se carga sola. Las partes 5 y 6 usarán
//     esto para botones "Ver en el mapa" que apuntan a esa dirección.
//   · al SALIR de la pantalla se cancelan los temporizadores y se suelta el mapa (función de limpieza).

import { CONFIG } from "../../config.js";
import { apiGet, SesionExpiradaError } from "../api.js";
import { iniciarMapa, destruirMapa, dibujarRuta, enfocarParada, tieneCoordenadas, tipoDeParada } from "../mapa.js";
import { crearCampo, enviarFormulario, datosFormulario } from "../ui/formulario.js";
import { escribirMensaje } from "../ui/mensajes.js";
import { el, boton, encabezadoVista } from "../ui/util.js";

const MAX_REFRESCOS = 20; // tope de reconsultas automáticas (20 x 30 s = 10 min): no se consulta para siempre

export async function montar(contenedor, { params }) {
    let activa = true;            // pasa a false al salir de la pantalla: frena cualquier cosa que llegue tarde
    let temporizador = null;
    let promesaMapa = null;

    // ----------------------------- Estructura -----------------------------
    const campoCodigo = crearCampo({
        nombre: "codigo", etiqueta: "Código de ruta", requerido: true, placeholder: "Ej. PRUEBA-001",
        atributos: { autocomplete: "off", spellcheck: "false" },
    });
    const formulario = el("form", { class: "mapa-formulario" },
        campoCodigo.contenedor,
        boton("Ver ruta", { variante: "primario", tipo: "submit" }));

    // role="status": el lector de pantalla anuncia los cambios ("Cargando ruta...", "4 paradas en el mapa")
    const mensaje = el("p", { class: "mensaje", role: "status" });
    const resumen = el("p", { class: "mapa-resumen" });
    const lista = el("ol", { class: "paradas", "aria-label": "Paradas de la ruta" });
    const zonaMapa = el("div", { class: "mapa", role: "region", "aria-label": "Mapa de la ruta" });

    contenedor.append(
        encabezadoVista({
            sobretitulo: "Rutas", antes: "Mapa de ", destacado: "ruta",
            descripcion: "Escribe el código de una ruta para ver sus paradas en orden.",
        }),
        formulario,
        el("div", { class: "mapa-layout" },
            el("aside", { class: "mapa-panel" }, mensaje, resumen, lista),
            zonaMapa));

    // ------------------------------- Mapa --------------------------------
    // El mapa se crea la PRIMERA vez que se necesita, ya con el <div> dentro de la página
    // (dentro de un contenedor oculto o sin montar tendría tamaño 0).
    function asegurarMapa() {
        promesaMapa ??= iniciarMapa(zonaMapa, (texto) => escribirMensaje(mensaje, texto, "error"))
            .catch((error) => { promesaMapa = null; throw error; }); // si falla, permite reintentar
        return promesaMapa;
    }

    // ------------------------------- Datos --------------------------------
    // Acepta tanto una lista directa de paradas como un objeto { paradas: [...] }, y las ordena por ordenParada.
    // Si la respuesta real del backend tiene otra forma, SOLO se cambia esta función.
    function normalizarParadas(json) {
        const paradas = Array.isArray(json) ? json : (json.paradas ?? json.trayectos ?? []);
        return [...paradas].sort((a, b) => a.ordenParada - b.ordenParada);
    }

    function pintarResumen(paradas) {
        const primera = paradas[0];
        const conductor = primera.conductor
            ? `${primera.conductor.nombres ?? ""} ${primera.conductor.apellidos ?? ""}`.trim() : "—";
        const placa = primera.vehiculo?.placa ?? "—";
        resumen.textContent = `Conductor: ${conductor} · Vehículo: ${placa} · Registrada por: ${primera.loginUsuario ?? "—"}`;
    }

    // Cada parada es un <button> dentro del <li>: así se puede activar con teclado (Enter/Espacio), no solo con el mouse
    function pintarLista(paradas) {
        lista.replaceChildren(...paradas.map((parada, indice) => {
            const tipo = tipoDeParada(indice, paradas.length);
            const conCoord = tieneCoordenadas(parada);
            return el("li", {},
                el("button", {
                    type: "button", class: `parada ${tipo}${conCoord ? "" : " pendiente"}`,
                    onclick: () => enfocarParada(indice),
                },
                    el("span", { class: "parada-numero", "aria-hidden": "true" }, String(indice + 1)),
                    el("span", {},
                        // Inicio/Fin también como TEXTO: el significado no depende solo del color del pin
                        tipo === "inicio" ? el("span", { class: "parada-rol" }, "Inicio")
                            : tipo === "fin" ? el("span", { class: "parada-rol" }, "Fin") : null,
                        el("span", { class: "parada-lugar" }, parada.ubicacion),      // textContent: nunca innerHTML con datos de la base
                        el("br"),
                        el("span", { class: "parada-coord" }, conCoord
                            ? `${Number(parada.latitud).toFixed(6)}, ${Number(parada.longitud).toFixed(6)}`
                            : "Coordenadas pendientes"))));
        }));
    }

    function limpiarResultado() {
        lista.replaceChildren();
        resumen.textContent = "";
    }

    // intento = 0 es una consulta manual; intento > 0 es una reconsulta automática
    async function cargarRuta(codigo, intento = 0) {
        clearTimeout(temporizador);
        if (intento === 0) { limpiarResultado(); escribirMensaje(mensaje, "Cargando ruta..."); }

        try {
            const json = await apiGet(`/api/trayectos/rutas/${encodeURIComponent(codigo)}`);
            if (!activa) return;

            const paradas = normalizarParadas(json);
            if (paradas.length === 0) { escribirMensaje(mensaje, "Esa ruta no tiene paradas.", "error"); return; }

            await asegurarMapa();
            if (!activa) return;
            pintarResumen(paradas);
            pintarLista(paradas);
            // En una reconsulta automática NO se reencuadra: si el usuario hizo zoom, el mapa no debe saltar
            const enMapa = dibujarRuta(paradas, { encuadrar: intento === 0 });
            const pendientes = paradas.length - enMapa;

            if (pendientes === 0) {
                escribirMensaje(mensaje, `Ruta ${codigo}: ${paradas.length} paradas en el mapa.`);
            } else if (intento < MAX_REFRESCOS) {
                // La tarea programada del backend completa las coordenadas cada 90 s: volvemos a preguntar hasta que estén
                escribirMensaje(mensaje, `${pendientes} parada(s) sin coordenadas todavía. Se consulta de nuevo sola cada ${CONFIG.REFRESCO_MS / 1000} s.`);
                temporizador = setTimeout(() => cargarRuta(codigo, intento + 1), CONFIG.REFRESCO_MS);
            } else {
                escribirMensaje(mensaje, `${pendientes} parada(s) siguen sin coordenadas. Pulsa "Ver ruta" para consultar de nuevo.`, "error");
            }
        } catch (error) {
            if (!activa || error instanceof SesionExpiradaError) return; // app.js ya lleva al login
            escribirMensaje(mensaje, error.message, "error");
        }
    }

    // ------------------------------ Eventos -------------------------------
    enviarFormulario(formulario, {
        textoCargando: "Consultando…",
        alEnviar: async (form) => {
            const { codigo } = datosFormulario(form);
            // Deja el código en la URL SIN recargar la pantalla (replaceState no dispara "hashchange"):
            // así se puede copiar el enlace o recargar con F5 y la ruta vuelve a cargarse sola.
            history.replaceState(null, "", `#/mapa/${encodeURIComponent(codigo)}`);
            await cargarRuta(codigo);
        },
    });

    // Si la URL ya trae el código (#/mapa/PRUEBA-001), se carga sin que el usuario tenga que hacer nada
    if (params.codigo) {
        campoCodigo.entrada.value = params.codigo;
        cargarRuta(params.codigo);
    } else {
        // Sin código no hay nada que dibujar, pero el mapa se prepara para que aparezca ya
        asegurarMapa().catch((error) => { if (activa) escribirMensaje(mensaje, error.message, "error"); });
        campoCodigo.entrada.focus();
    }

    // ---------------------- Limpieza al salir de la pantalla ----------------------
    return () => {
        activa = false;
        clearTimeout(temporizador);
        destruirMapa();
    };
}

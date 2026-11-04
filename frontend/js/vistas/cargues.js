// ============================================================================
// vistas/cargues.js · CARGUE MASIVO DE TRAYECTOS DESDE EXCEL  (#/cargues  y  #/cargues/202611041530)
// ============================================================================
// Backend que usa (los dos protegidos con token + APIKey):
//   POST /api/trayectos/cargues               multipart, campo "archivo"  ->  202 { idCargue, totalFilas, estado, mensaje }
//   GET  /api/trayectos/cargues/{idCargue}    -> { idCargue, total, resumen: {CARGADO, VALIDADO, PROCESADO, ERROR}, registros: [...] }
//
// El POST NO espera a que se validen las filas: las guarda en estado CARGADO, publica un mensaje en
// RabbitMQ y responde 202 de inmediato. Un consumidor en segundo plano ejecuta sp_validacion y
// sp_procesado. Por eso, tras subir, esta pantalla CONSULTA cada pocos segundos hasta que no quede
// ninguna fila en CARGADO, y mientras tanto muestra una barra con el avance real.
//
// Un solo camino para ver un resultado: la dirección #/cargues/{idCargue}.
//   · al terminar de subir un archivo se navega ahí
//   · el buscador de cargues anteriores navega ahí
//   · y como es una dirección, "Atrás" y recargar la página (F5) también funcionan.

import { apiGet, apiSubir, SesionExpiradaError } from "../api.js";
import { el, boton, etiqueta, encabezadoVista } from "../ui/util.js";
import { crearTabla } from "../ui/tabla.js";
import { avisar, anunciar, estadoCarga, estadoError, estadoVacio } from "../ui/mensajes.js";
import { crearCampo, enviarFormulario, datosFormulario, mostrarErrorGeneral } from "../ui/formulario.js";

const RUTA_PLANTILLA = "plantilla/plantilla_cargue_trayectos.xlsx";
const TAMANO_MAX_BYTES = 2 * 1024 * 1024; // igual que spring.servlet.multipart.max-file-size=2MB
const INTERVALO_CONSULTA_MS = 1500;       // cada cuánto se pregunta por el avance mientras hay filas en CARGADO
const MAX_CONSULTAS = 40;                 // 40 x 1,5 s = 1 minuto. Si sigue en CARGADO, algo falló en el consumidor
const PATRON_ID = /^\d{12}$/;             // yyyyMMddHHmm

// Texto, color de etiqueta y explicación de cada estado de trayecto_tmp
const ESTADOS = {
    CARGADO: { texto: "Cargado", tipo: "info", ayuda: "Esperando la validación" },
    VALIDADO: { texto: "Validado", tipo: "aviso", ayuda: "Válidas, pendientes de traslado" },
    PROCESADO: { texto: "Procesado", tipo: "ok", ayuda: "Ya están en trayectos" },
    ERROR: { texto: "Error", tipo: "error", ayuda: "No pasaron la validación" },
};
const ORDEN_ESTADOS = Object.keys(ESTADOS);

const chipEstado = (estado) => etiqueta(ESTADOS[estado]?.texto ?? estado ?? "—", ESTADOS[estado]?.tipo ?? "");
const numero = new Intl.NumberFormat("es-CO");

// 1536 -> "1,5 KB"
function formatearTamano(bytes) {
    if (bytes < 1024) return `${bytes} B`;
    const kb = bytes / 1024;
    return kb < 1024 ? `${kb.toFixed(1).replace(".", ",")} KB` : `${(kb / 1024).toFixed(1).replace(".", ",")} MB`;
}

// 202611041530 -> "4 nov 2026, 3:30 p. m."  (el id ES la fecha y hora del cargue)
function fechaDelId(id) {
    const m = /^(\d{4})(\d{2})(\d{2})(\d{2})(\d{2})$/.exec(String(id));
    if (!m) return "";
    const fecha = new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]), Number(m[4]), Number(m[5]));
    return fecha.toLocaleString("es-CO", { day: "numeric", month: "short", year: "numeric", hour: "numeric", minute: "2-digit" });
}

// Se revisa ANTES de enviar: así el usuario no espera una subida que el servidor va a rechazar.
// El servidor vuelve a validar todo (extensión, hoja, encabezados, filas): esto es solo una ayuda.
function validarArchivo(archivo) {
    if (!archivo) return "Selecciona un archivo .xlsx.";
    if (!archivo.name.toLowerCase().endsWith(".xlsx")) return "El archivo debe ser un Excel con extensión .xlsx.";
    if (archivo.size === 0) return "El archivo está vacío.";
    if (archivo.size > TAMANO_MAX_BYTES) return `El archivo pesa ${formatearTamano(archivo.size)}; el máximo es 2 MB.`;
    return null;
}

export function montar(contenedor, { params, navegar }) {
    let activa = true;          // false al salir de la pantalla: frena consultas que lleguen tarde
    let temporizador = null;    // la siguiente consulta de avance
    let archivoElegido = null;

    // ======================= 1. NUEVO CARGUE (3 pasos) =======================
    const entradaArchivo = el("input", {
        type: "file", id: "cargue-archivo", class: "solo-lectores",
        accept: ".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "aria-describedby": "cargue-archivo-ayuda cargue-archivo-error",
    });
    const nombreArchivo = el("p", { class: "soltar-archivo" });
    const errorArchivo = el("p", { class: "campo-error", id: "cargue-archivo-error", role: "alert", hidden: true });
    const botonQuitar = boton("Quitar archivo", { chico: true, hidden: true, onclick: () => elegirArchivo(null) });

    // La zona de soltar ES la etiqueta del <input type="file">: con el teclado se llega al input
    // (Tab) y se abre con Enter/Espacio; con el ratón se hace clic o se arrastra el archivo encima.
    const zonaSoltar = el("label", { class: "soltar", for: "cargue-archivo" },
        el("span", { class: "soltar-icono", "aria-hidden": "true" }, "⬆"),
        el("span", { class: "soltar-titulo" }, "Arrastra aquí tu Excel o ", el("u", {}, "elige un archivo")),
        el("span", { class: "texto-tenue", id: "cargue-archivo-ayuda" }, "Solo .xlsx · máximo 2 MB · entre 1 y 500 filas"),
        nombreArchivo);

    const progreso = el("progress", { class: "progreso", hidden: true });
    const estadoSubida = el("p", { class: "mensaje", role: "status" });
    const botonEnviar = boton("Enviar cargue", { variante: "primario", tipo: "submit" });

    const formularioCargue = el("form", { class: "formulario", "aria-labelledby": "titulo-nuevo-cargue" },
        el("ol", { class: "pasos" },
            el("li", { class: "paso" },
                el("h3", {}, "Descarga la plantilla"),
                el("p", { class: "texto-tenue" }, "Trae los encabezados exactos y una hoja con instrucciones."),
                el("a", { class: "boton boton--contorno boton--chico", href: RUTA_PLANTILLA, download: "plantilla_cargue_trayectos.xlsx" },
                    "Descargar plantilla")),
            el("li", { class: "paso" },
                el("h3", {}, "Elige el archivo"),
                entradaArchivo, zonaSoltar, errorArchivo, botonQuitar),
            el("li", { class: "paso" },
                el("h3", {}, "Envíalo"),
                el("p", { class: "texto-tenue" }, "Las filas se validan en segundo plano; el resultado aparece abajo."),
                el("div", { class: "formulario-acciones" }, botonEnviar),
                progreso, estadoSubida)));

    function elegirArchivo(archivo) {
        archivoElegido = archivo;
        if (!archivo) entradaArchivo.value = "";
        const error = archivo ? validarArchivo(archivo) : null;
        errorArchivo.textContent = error ?? "";
        errorArchivo.hidden = !error;
        zonaSoltar.classList.toggle("soltar--error", Boolean(error));
        zonaSoltar.classList.toggle("soltar--lista", Boolean(archivo && !error));
        nombreArchivo.textContent = archivo ? `${archivo.name} · ${formatearTamano(archivo.size)}` : "";
        botonQuitar.hidden = !archivo;
        if (archivo && !error) anunciar(`Archivo elegido: ${archivo.name}`);
    }

    entradaArchivo.addEventListener("change", () => elegirArchivo(entradaArchivo.files[0] ?? null));

    // Arrastrar y soltar. dragenter/dragleave se disparan también al pasar por los hijos de la zona,
    // por eso se cuenta cuántas veces se entró en lugar de quitar la clase en el primer dragleave.
    let entradasArrastre = 0;
    zonaSoltar.addEventListener("dragenter", (e) => { e.preventDefault(); entradasArrastre++; zonaSoltar.classList.add("soltar--encima"); });
    zonaSoltar.addEventListener("dragover", (e) => { e.preventDefault(); e.dataTransfer.dropEffect = "copy"; });
    zonaSoltar.addEventListener("dragleave", () => { if (--entradasArrastre <= 0) { entradasArrastre = 0; zonaSoltar.classList.remove("soltar--encima"); } });
    zonaSoltar.addEventListener("drop", (e) => {
        e.preventDefault();
        entradasArrastre = 0;
        zonaSoltar.classList.remove("soltar--encima");
        const archivos = e.dataTransfer.files;
        if (archivos.length > 1) { elegirArchivo(null); mostrarErrorArchivo("Suelta un solo archivo."); return; }
        elegirArchivo(archivos[0] ?? null);
    });
    // Si el archivo cae FUERA de la zona, el navegador lo abriría y se perdería la aplicación: se evita
    // mientras esta pantalla está abierta (se quita en la limpieza).
    const evitarAbrir = (e) => { if (!zonaSoltar.contains(e.target)) e.preventDefault(); };
    window.addEventListener("dragover", evitarAbrir);
    window.addEventListener("drop", evitarAbrir);

    function mostrarErrorArchivo(texto) {
        errorArchivo.textContent = texto;
        errorArchivo.hidden = false;
        zonaSoltar.classList.add("soltar--error");
    }

    enviarFormulario(formularioCargue, {
        textoCargando: "Enviando…",
        alEnviar: async () => {
            const error = validarArchivo(archivoElegido);
            if (error) { mostrarErrorArchivo(error); entradaArchivo.focus(); return; }

            const datos = new FormData();
            datos.append("archivo", archivoElegido); // el nombre "archivo" es el @RequestParam del controlador
            progreso.hidden = false;
            progreso.removeAttribute("value");       // sin value = barra indeterminada (no se sabe cuánto falta)
            estadoSubida.textContent = "Subiendo el archivo…";
            try {
                const respuesta = await apiSubir("/api/trayectos/cargues", datos);
                if (!activa) return;
                avisar(`Cargue ${respuesta.idCargue} recibido: ${respuesta.totalFilas} filas en validación.`, "ok");
                elegirArchivo(null);
                navegar(`/cargues/${respuesta.idCargue}`);
            } catch (e) {
                if (!activa || e instanceof SesionExpiradaError) return;
                // Errores del servidor que se explican solos: "Ya hay un cargue en este minuto",
                // "El archivo no contiene la hoja llamada 'Trayectos'", "encabezados", 413 por tamaño...
                mostrarErrorGeneral(formularioCargue, e.message || "No se pudo enviar el archivo.");
            } finally {
                if (activa) { progreso.hidden = true; estadoSubida.textContent = ""; }
            }
        },
    });

    // ===================== 2. CONSULTAR UN CARGUE ANTERIOR ====================
    const campoId = crearCampo({
        nombre: "idCargue", etiqueta: "Identificador del cargue", requerido: true, valor: params.idCargue ?? "",
        placeholder: "Ej. 202611041530", ayuda: "Son 12 dígitos: año, mes, día, hora y minuto del cargue.",
        atributos: { inputmode: "numeric", pattern: "\\d{12}", maxlength: "12", autocomplete: "off",
            title: "Escribe los 12 dígitos del identificador, sin espacios." },
    });
    const formularioBuscar = el("form", { class: "formulario buscar-cargue", "aria-label": "Buscar un cargue anterior" },
        campoId.contenedor,
        el("div", { class: "formulario-acciones" }, boton("Buscar", { variante: "primario", tipo: "submit" })));
    enviarFormulario(formularioBuscar, {
        textoCargando: "Buscando…",
        alEnviar: async (form) => {
            const { idCargue } = datosFormulario(form);
            if (idCargue === params.idCargue) { mostrarCargue(idCargue, { enfocar: true }); return; } // misma dirección: se recarga a mano
            navegar(`/cargues/${idCargue}`);
        },
    });

    // ============================== 3. RESULTADO ==============================
    const zonaResultado = el("section", { class: "resultado cargue-resultado", "aria-labelledby": "titulo-resultado-cargue", hidden: true });

    contenedor.append(
        encabezadoVista({
            sobretitulo: "Entrega 4", antes: "Cargue ", destacado: "masivo",
            descripcion: "Sube un Excel con rutas y sus paradas. Las rutas correctas pasan a trayectos y las filas con errores quedan con su observación.",
        }),
        el("div", { class: "cargue-layout" },
            el("section", { class: "tarjeta", "aria-labelledby": "titulo-nuevo-cargue" },
                el("h2", { id: "titulo-nuevo-cargue", class: "titulo-seccion" }, "Nuevo cargue"),
                formularioCargue),
            el("section", { class: "tarjeta", "aria-labelledby": "titulo-buscar-cargue" },
                el("h2", { id: "titulo-buscar-cargue", class: "titulo-seccion" }, "Cargues anteriores"),
                el("p", { class: "texto-tenue" }, "El identificador aparece al enviar un archivo."),
                formularioBuscar)),
        zonaResultado);

    // ------------------------- Consulta y avance ------------------------------
    async function mostrarCargue(id, { enfocar = false } = {}) {
        clearTimeout(temporizador);
        zonaResultado.hidden = false;
        zonaResultado.replaceChildren(estadoCarga(`Consultando el cargue ${id}…`));
        if (enfocar) zonaResultado.scrollIntoView({ behavior: "smooth", block: "start" });
        await consultar(id, 1, enfocar);
    }

    async function consultar(id, intento, enfocar) {
        let datos;
        try {
            datos = await apiGet(`/api/trayectos/cargues/${id}`);
        } catch (error) {
            if (!activa || error instanceof SesionExpiradaError) return;
            zonaResultado.replaceChildren(error.estado === 404
                ? estadoVacio(`No existe el cargue ${id}`, "Revisa el identificador: son los 12 dígitos que se mostraron al enviar el archivo.")
                : estadoError(error.message, () => mostrarCargue(id)));
            return;
        }
        if (!activa) return;
        // Defensa ante una respuesta con otra forma (proxy, versión vieja del backend): sin resumen no hay qué dibujar
        if (!datos || typeof datos.resumen !== "object" || datos.resumen === null) {
            zonaResultado.replaceChildren(estadoError("El servidor respondió algo inesperado al consultar el cargue.", () => mostrarCargue(id)));
            return;
        }

        const pendientes = datos.resumen.CARGADO ?? 0;
        if (pendientes > 0 && intento < MAX_CONSULTAS) {
            dibujarEnProceso(datos);
            temporizador = setTimeout(() => consultar(id, intento + 1, enfocar), INTERVALO_CONSULTA_MS);
            if (intento === 1 && enfocar) enfocarTitulo();
            return;
        }

        dibujarResultado(datos, { sinTerminar: pendientes > 0 });
        if (pendientes === 0) {
            const r = datos.resumen;
            anunciar(`Cargue ${id} terminado: ${r.PROCESADO} filas procesadas y ${r.ERROR} con error.`);
        }
        if (enfocar || intento > 1) enfocarTitulo();
    }

    function enfocarTitulo() {
        zonaResultado.querySelector("#titulo-resultado-cargue")?.focus({ preventScroll: true });
        zonaResultado.scrollIntoView({ behavior: "smooth", block: "start" });
    }

    function cabeceraResultado(datos, extra = null) {
        const usuario = datos.registros?.[0]?.loginUsuario;
        return el("div", { class: "resultado-cabecera" },
            el("div", {},
                el("p", { class: "sobretitulo" }, "Resultado"),
                el("h2", { id: "titulo-resultado-cargue", tabindex: "-1" }, "Cargue ", el("span", { class: "mono" }, String(datos.idCargue))),
                el("p", { class: "texto-tenue" },
                    [fechaDelId(datos.idCargue), `${numero.format(datos.total)} filas`, usuario ? `por ${usuario}` : null]
                        .filter(Boolean).join(" · "))),
            extra);
    }

    function tarjetasResumen(resumen) {
        return el("ul", { class: "cargue-resumen", "aria-label": "Filas por estado" },
            ORDEN_ESTADOS.map((estado) => el("li", { class: `tarjeta conteo conteo--${ESTADOS[estado].tipo}` },
                chipEstado(estado),
                el("span", { class: "conteo-numero" }, numero.format(resumen?.[estado] ?? 0)),
                el("span", { class: "texto-tenue" }, ESTADOS[estado].ayuda))));
    }

    // Mientras el consumidor trabaja: tarjetas + barra con el avance real (filas que ya salieron de CARGADO)
    function dibujarEnProceso(datos) {
        const hechas = datos.total - datos.resumen.CARGADO;
        zonaResultado.replaceChildren(
            cabeceraResultado(datos, etiqueta("Validando…", "info")),
            el("div", { class: "cargue-avance" },
                el("progress", { class: "progreso", max: String(datos.total), value: String(hechas), "aria-label": "Avance de la validación" }),
                el("p", { class: "texto-tenue", role: "status" },
                    `Validando en segundo plano: ${numero.format(hechas)} de ${numero.format(datos.total)} filas revisadas.`)),
            tarjetasResumen(datos.resumen));
    }

    function dibujarResultado(datos, { sinTerminar }) {
        const registros = (datos.registros ?? []).map((r, i) => ({ ...r, numero: i + 1 }));
        const resumen = datos.resumen ?? {};

        const aviso = sinTerminar
            ? el("div", { class: "formulario-error", role: "alert" },
                `Después de un minuto aún hay ${resumen.CARGADO} filas sin validar. Es posible que el procesamiento en segundo plano haya fallado `,
                "(revisa que RabbitMQ esté encendido y la cola de mensajes fallidos cargue.excel.dlq). ",
                boton("Volver a consultar", { chico: true, onclick: () => mostrarCargue(datos.idCargue, { enfocar: true }) }))
            : null;

        // Rutas que quedaron en trayectos: un botón por código, que abre el mapa
        const rutasProcesadas = [...new Set(registros.filter((r) => r.estado === "PROCESADO").map((r) => r.codigoRuta))];
        const bloqueRutas = rutasProcesadas.length
            ? el("div", { class: "cargue-rutas" },
                el("h3", {}, rutasProcesadas.length === 1 ? "Ruta creada" : `${rutasProcesadas.length} rutas creadas`),
                el("ul", { class: "cargue-rutas-lista" }, rutasProcesadas.map((codigo) => el("li", {},
                    el("a", { class: "boton boton--contorno boton--chico", href: `#/mapa/${encodeURIComponent(codigo)}`,
                        "aria-label": `Ver la ruta ${codigo} en el mapa` },
                        el("span", { class: "mono" }, codigo), " · Ver en el mapa")))))
            : null;

        // Filtro por estado: solo se ofrecen los estados que tienen filas
        let filtro = "TODOS";
        const opciones = [{ valor: "TODOS", texto: `Todas (${registros.length})` },
            ...ORDEN_ESTADOS.filter((e) => resumen[e] > 0).map((e) => ({ valor: e, texto: `${ESTADOS[e].texto} (${resumen[e]})` }))];
        const selectorFiltro = el("select", { id: "filtro-estado-cargue" },
            opciones.map((o) => el("option", { value: o.valor }, o.texto)));
        const contadorFiltro = el("p", { class: "texto-tenue", role: "status" });
        const zonaTabla = el("div");

        function dibujarTabla() {
            const visibles = filtro === "TODOS" ? registros : registros.filter((r) => r.estado === filtro);
            contadorFiltro.textContent = `Mostrando ${visibles.length} de ${registros.length} filas.`;
            zonaTabla.replaceChildren(crearTabla({
                titulo: `Filas del cargue ${datos.idCargue}`,
                filas: visibles,
                vacio: { titulo: "No hay filas con ese estado" },
                columnas: [
                    { titulo: "#", clase: "mono texto-tenue", render: (r) => String(r.numero) },
                    { titulo: "Ruta", clase: "mono", render: (r) => r.codigoRuta },
                    { titulo: "Orden", render: (r) => r.ordenParada },
                    { titulo: "Ubicación", render: (r) => r.ubicacion },
                    { titulo: "Conductor", render: (r) => r.idPersona },
                    { titulo: "Vehículo", render: (r) => r.idVehiculo },
                    { titulo: "Coordenadas", clase: "mono", render: (r) => (r.latitud || r.longitud)
                        ? `${r.latitud ?? "—"}, ${r.longitud ?? "—"}` : el("span", { class: "texto-tenue" }, "Las completa Google Maps") },
                    { titulo: "Estado", render: (r) => chipEstado(r.estado) },
                    { titulo: "Observación", clase: "celda-observacion", render: (r) => celdaObservacion(r.observacion) },
                ],
            }));
        }
        selectorFiltro.addEventListener("change", () => { filtro = selectorFiltro.value; dibujarTabla(); });
        dibujarTabla();

        // OJO: replaceChildren (a diferencia de el()) convierte null en el texto "null": se filtran antes
        zonaResultado.replaceChildren(...[
            cabeceraResultado(datos, sinTerminar ? etiqueta("Sin terminar", "aviso") : etiqueta("Terminado", "ok")),
            aviso,
            tarjetasResumen(resumen),
            bloqueRutas,
            el("div", { class: "cargue-filtro" },
                el("div", { class: "campo" },
                    el("label", { for: "filtro-estado-cargue" }, "Mostrar filas"),
                    selectorFiltro),
                contadorFiltro),
            zonaTabla,
        ].filter(Boolean));
    }

    // La observación acumula varios errores separados por "; " (CONCAT_WS en sp_validacion): uno por línea
    function celdaObservacion(texto) {
        if (!texto) return el("span", { class: "texto-tenue" }, "—");
        const partes = texto.split(";").map((p) => p.trim()).filter(Boolean);
        return partes.length === 1 ? partes[0] : el("ul", { class: "observaciones" }, partes.map((p) => el("li", {}, p)));
    }

    // Llegó con un id en la dirección (#/cargues/202611041530): se muestra de una vez
    if (params.idCargue) {
        if (PATRON_ID.test(params.idCargue)) mostrarCargue(params.idCargue, { enfocar: true });
        else {
            zonaResultado.hidden = false;
            zonaResultado.replaceChildren(estadoVacio("Identificador no válido", "Debe tener 12 dígitos, por ejemplo 202611041530."));
        }
    }

    // Limpieza al salir de la pantalla
    return () => {
        activa = false;
        clearTimeout(temporizador);
        window.removeEventListener("dragover", evitarAbrir);
        window.removeEventListener("drop", evitarAbrir);
    };
}

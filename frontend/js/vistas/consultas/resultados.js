// ============================================================================
// vistas/consultas/resultados.js · CÓMO SE DIBUJA EL RESULTADO DE CADA CONSULTA
// ============================================================================
// Cada función recibe los datos que respondió el backend y devuelve un NODO listo para
// meter en la página. Aquí NO se llama al servidor: eso lo hace consultasPublicas.js.
// Todo texto que viene de la base de datos entra con el()/textContent (nunca innerHTML).

import { obtenerSesion } from "../../session.js";
import { el, etiqueta, formatearFecha, diasHasta } from "../../ui/util.js";
import { crearTabla } from "../../ui/tabla.js";
import {
    TIPO_VEHICULO, TIPO_SERVICIO, TIPO_COMBUSTIBLE, MOTIVO_RESTRICCION,
    textoDe, chipEstadoDocumento, chipEstadoConductor, textoDias,
} from "../../ui/catalogos.js";

// ------------------------------ Piezas pequeñas ------------------------------

// "Chevrolet Spark · 2020"
const descripcionVehiculo = (v) =>
    [[v.marca, v.linea].filter(Boolean).join(" "), v.modelo].filter(Boolean).join(" · ") || "—";

// Lista de pares "Etiqueta: valor" (<dl>), el elemento correcto para fichas de datos
function fichaDatos(pares) {
    return el("dl", { class: "datos" }, pares.map(([nombre, valor]) =>
        el("div", {}, el("dt", { class: "etiqueta-texto" }, nombre), el("dd", {}, valor ?? "—"))));
}

// ---------------------- 1 y 2 · Vehículos con documentos ----------------------
// `resaltar(documento)` decide qué documentos son "la razón" por la que el vehículo salió en la consulta:
//   devuelve { tipo, texto } (la etiqueta de color a mostrar) o null si ese documento no es relevante.
// Los demás documentos del vehículo se resumen en una línea ("+2 sin novedad").
export function tablaVehiculosConDocumentos(vehiculos, resaltar, tituloTabla) {
    return crearTabla({
        titulo: tituloTabla,
        columnas: [
            { titulo: "Placa", render: (v) => el("strong", { class: "mono" }, v.placa) },
            { titulo: "Vehículo", render: (v) => el("span", {},
                descripcionVehiculo(v), el("br"),
                el("span", { class: "texto-tenue" }, `${textoDe(TIPO_VEHICULO, v.tipoVehiculo)} · ${textoDe(TIPO_SERVICIO, v.tipoServicio)}`)) },
            { titulo: "Documentos que requieren atención", render: (v) => celdaDocumentos(v, resaltar) },
        ],
        filas: vehiculos,
    });
}

function celdaDocumentos(vehiculo, resaltar) {
    const todos = vehiculo.documentos ?? [];
    const relevantes = todos
        .map((d) => ({ d, marca: resaltar(d) }))
        .filter((x) => x.marca);
    const resto = todos.length - relevantes.length;

    return el("div", { class: "docs-celda" },
        el("ul", { class: "docs-lista" }, relevantes.map(({ d, marca }) =>
            el("li", {},
                el("span", {}, d.nombreDocumento ?? d.codigoDocumento ?? "Documento"),
                etiqueta(marca.texto, marca.tipo),
                el("span", { class: "texto-tenue" }, `vence ${formatearFecha(d.fechaVencimiento)}`)))),
        resto > 0 ? el("p", { class: "texto-tenue" }, `+${resto} documento${resto === 1 ? "" : "s"} sin novedad`) : null);
}

// Reglas de "relevante" para cada consulta
export const resaltarVencidos = (doc) =>
    doc.estadoDocumento === "Vencido" ? { tipo: "error", texto: "Vencido" } : null;

export const resaltarPorVencer = (dias) => (doc) => {
    const faltan = diasHasta(doc.fechaVencimiento);
    return faltan !== null && faltan >= 0 && faltan <= dias ? { tipo: "aviso", texto: textoDias(faltan) } : null;
};

// ------------------------- 3 · Conductores disponibles ------------------------
export function tablaConductores(personas) {
    return crearTabla({
        titulo: "Conductores que pueden operar",
        columnas: [
            { titulo: "Identificación", clase: "mono", render: (p) => p.identificacion },
            { titulo: "Nombre", render: (p) => `${p.nombres ?? ""} ${p.apellidos ?? ""}`.trim() || "—" },
            { titulo: "Correo", render: (p) => p.correoElectronico },
            { titulo: "Licencia vigente hasta", render: (p) => p.fechaVigenciaLicencia
                ? el("span", {}, formatearFecha(p.fechaVigenciaLicencia), " ",
                    el("span", { class: "texto-tenue" }, `(${textoDias(diasHasta(p.fechaVigenciaLicencia)).toLowerCase()})`))
                : "—" },
        ],
        filas: personas,
    });
}

// ----------------------------- 4 · Vehículo por placa -------------------------
export function fichaVehiculoCompleto(v) {
    const documentos = crearTabla({
        titulo: `Documentos del vehículo ${v.placa}`,
        columnas: [
            { titulo: "Documento", render: (d) => `${d.codigoDocumento ?? ""} ${d.nombreDocumento ?? ""}`.trim() || "—" },
            { titulo: "Expedición", render: (d) => formatearFecha(d.fechaExpedicion) },
            { titulo: "Vencimiento", render: (d) => formatearFecha(d.fechaVencimiento) },
            { titulo: "Estado", render: (d) => chipEstadoDocumento(d.estadoDocumento) },
            { titulo: "Tiempo", render: (d) => textoDias(diasHasta(d.fechaVencimiento)) || "—" },
        ],
        filas: v.documentos ?? [],
        vacio: { titulo: "Sin documentos", texto: "Este vehículo no tiene documentos asociados." },
    });

    const conductores = crearTabla({
        titulo: `Conductores asociados al vehículo ${v.placa}`,
        columnas: [
            { titulo: "Identificación", clase: "mono", render: (c) => c.identificacion },
            { titulo: "Nombre", render: (c) => `${c.nombres ?? ""} ${c.apellidos ?? ""}`.trim() },
            { titulo: "Asociado desde", render: (c) => formatearFecha(c.fechaAsociacion) },
            { titulo: "Estado", render: (c) => chipEstadoConductor(c.estadoConductor) },
        ],
        filas: v.conductores ?? [],
        vacio: { titulo: "Sin conductores", texto: "Este vehículo aún no tiene conductores asociados." },
    });

    return el("article", { class: "detalle-vehiculo" },
        el("header", { class: "detalle-cabecera" },
            el("p", { class: "detalle-placa" }, v.placa),
            el("div", { class: "detalle-chips" },
                etiqueta(textoDe(TIPO_VEHICULO, v.tipoVehiculo), "info"),
                etiqueta(textoDe(TIPO_SERVICIO, v.tipoServicio)))),
        fichaDatos([
            ["Marca", v.marca], ["Línea", v.linea], ["Modelo", v.modelo], ["Color", v.color],
            ["Combustible", textoDe(TIPO_COMBUSTIBLE, v.tipoCombustible)],
            ["Capacidad", v.capacidadPasajeros != null ? `${v.capacidadPasajeros} pasajeros` : "—"],
        ]),
        el("h3", {}, "Documentos"), documentos,
        el("h3", {}, "Conductores asociados"), conductores);
}

// ---------------------------- 5 · Personas por tipo ---------------------------
// El backend solo devuelve los tipos que TIENEN personas. Se muestran siempre los dos (con 0 si faltan).
export function tarjetasConteo(lista) {
    const totales = { C: 0, A: 0 };
    for (const fila of lista) {
        const codigo = fila.tipoPersona === "CONDUCTOR" ? "C" : fila.tipoPersona === "ADMINISTRATIVO" ? "A" : fila.tipoPersona;
        if (codigo in totales) totales[codigo] += Number(fila.total) || 0;
    }
    const nf = new Intl.NumberFormat("es-CO");
    const plurales = { C: "Conductores", A: "Administrativos" };
    return el("div", { class: "rejilla-tarjetas" },
        ["C", "A"].map((codigo) => el("div", { class: "tarjeta conteo" },
            el("span", { class: "etiqueta-texto" }, plurales[codigo]),
            el("span", { class: "conteo-numero" }, nf.format(totales[codigo])))));
}

export const totalConteo = (lista) => lista.reduce((suma, f) => suma + (Number(f.total) || 0), 0);

// --------------------------- 6 · Rutas restringidas ---------------------------
export function listaRutasRestringidas(rutas) {
    const haySesion = obtenerSesion() !== null; // el mapa es una pantalla protegida: el enlace solo sirve con sesión
    return el("div", { class: "rejilla-tarjetas rejilla-tarjetas--ancha" }, rutas.map((r) => {
        const paradas = [...(r.trayectos ?? [])].sort((a, b) => a.ordenParada - b.ordenParada);
        const conductor = r.conductor ? `${r.conductor.nombres ?? ""} ${r.conductor.apellidos ?? ""}`.trim() : "—";
        return el("article", { class: "tarjeta ruta-restringida" },
            el("header", { class: "ruta-cabecera" },
                el("h3", {}, r.codigoRuta),
                etiqueta(textoDe(MOTIVO_RESTRICCION, r.motivo), "error")),
            fichaDatos([["Conductor", conductor], ["Placa", r.placa], ["Paradas", String(paradas.length)]]),
            // <details> es un acordeón nativo: funciona con teclado y lector de pantalla sin JavaScript
            el("details", {},
                el("summary", {}, `Ver las ${paradas.length} paradas`),
                el("ol", { class: "ruta-paradas" }, paradas.map((p) => el("li", {}, p.ubicacion)))),
            haySesion
                ? el("a", { class: "boton boton--contorno boton--chico", href: `#/mapa/${encodeURIComponent(r.codigoRuta)}` }, "Ver en el mapa")
                : null);
    }));
}
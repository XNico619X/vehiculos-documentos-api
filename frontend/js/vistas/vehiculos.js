// ============================================================================
// vistas/vehiculos.js · FLOTA, DOCUMENTOS ASOCIADOS Y ARCHIVOS PDF
// ============================================================================
// La vista usa los contratos de VehiculoController:
//   GET/POST /api/vehiculos, GET/PUT/DELETE /api/vehiculos/{id}
//   POST /api/vehiculos/{id}/documentos
//   POST /api/vehiculos/{id}/documentos/pdf
//   PUT /api/vehiculos/{id}/documentos/{idDocumento}/estado
//
// Los documentos asociados se envían con fechas locales y los PDF como Base64
// puro (sin el prefijo data:application/pdf;base64,).

import { apiDelete, apiGet, apiPost, apiPut } from "../api.js";
import { ESTADO_DOCUMENTO, TIPO_COMBUSTIBLE, TIPO_SERVICIO, TIPO_VEHICULO, chipEstadoDocumento, textoDe, textoDias } from "../ui/catalogos.js";
import { confirmar } from "../ui/dialogo.js";
import { crearCampo, enviarFormulario, mostrarErrorCampo } from "../ui/formulario.js";
import { avisar, estadoCarga, estadoError, mostrarError } from "../ui/mensajes.js";
import { crearTabla } from "../ui/tabla.js";
import { boton, el, encabezadoVista, etiqueta, formatearFecha, diasHasta } from "../ui/util.js";

const TAMANO_MAX_PDF = 2 * 1024 * 1024;
const TIPOS_VEHICULO = [
    { valor: "AUTOMOVIL", texto: "Automóvil" },
    { valor: "MOTOCICLETA", texto: "Motocicleta" },
];
const TIPOS_SERVICIO = [
    { valor: "Pu", texto: "Público" },
    { valor: "Pr", texto: "Privado" },
];
const TIPOS_COMBUSTIBLE = [
    { valor: "Gasolina", texto: "Gasolina" },
    { valor: "Gas", texto: "Gas" },
    { valor: "Disel", texto: "Diésel" },
];
const ESTADOS_DOCUMENTO = Object.keys(ESTADO_DOCUMENTO).map((valor) => ({
    valor, texto: ESTADO_DOCUMENTO[valor].texto,
}));

export async function montar(contenedor) {
    let vehiculos = [];
    let catalogo = [];
    let detalleId = null;
    let busqueda = "";
    let filtroTipo = "";
    let filtroEstado = "";

    contenedor.append(encabezadoVista({
        sobretitulo: "Parte 3 · Gestión de flota",
        antes: "Vehículos y ",
        destacado: "documentos",
        descripcion: "Administra la flota, sus documentos y el estado de cada habilitación.",
    }));

    const espacioTrabajo = el("div", { class: "gestion-layout" });
    contenedor.append(espacioTrabajo);
    await cargarDatos();

    async function cargarDatos() {
        espacioTrabajo.replaceChildren(estadoCarga("Cargando vehículos y catálogo…"));
        try {
            [vehiculos, catalogo] = await Promise.all([
                apiGet("/api/vehiculos"),
                apiGet("/api/documentos"),
            ]);
            if (detalleId !== null && !vehiculos.some((v) => v.id === detalleId)) detalleId = null;
            dibujar();
        } catch (error) {
            espacioTrabajo.replaceChildren(estadoError(error.message, cargarDatos));
        }
    }

    function vehiculosFiltrados() {
        const termino = busqueda.trim().toLocaleLowerCase("es-CO");
        return vehiculos.filter((v) => {
            const coincidePlaca = !termino || String(v.placa ?? "").toLocaleLowerCase("es-CO").includes(termino);
            const coincideTipo = !filtroTipo || v.tipoVehiculo === filtroTipo;
            const coincideEstado = !filtroEstado || (v.documentos ?? []).some((d) => d.estadoDocumento === filtroEstado);
            return coincidePlaca && coincideTipo && coincideEstado;
        });
    }

    function dibujar() {
        const barra = el("section", { class: "gestion-barra tarjeta", "aria-label": "Búsqueda y filtros de vehículos" },
            crearCampo({ nombre: "buscarVehiculo", etiqueta: "Buscar por placa", tipo: "search", placeholder: "Ej. ABC123", valor: busqueda }).contenedor,
            crearCampo({
                nombre: "filtroTipo", etiqueta: "Tipo de vehículo", tipo: "select", valor: filtroTipo,
                opciones: TIPOS_VEHICULO,
            }).contenedor,
            crearCampo({
                nombre: "filtroEstado", etiqueta: "Estado del documento", tipo: "select", valor: filtroEstado,
                opciones: ESTADOS_DOCUMENTO,
            }).contenedor,
            boton("Nuevo vehículo", { variante: "primario", onclick: () => mostrarFormularioVehiculo() }));

        const entradaBusqueda = barra.querySelector('[name="buscarVehiculo"]');
        const selectorTipo = barra.querySelector('[name="filtroTipo"]');
        const selectorEstado = barra.querySelector('[name="filtroEstado"]');
        entradaBusqueda.addEventListener("input", () => { busqueda = entradaBusqueda.value; dibujarContenido(); });
        selectorTipo.addEventListener("change", () => { filtroTipo = selectorTipo.value; dibujarContenido(); });
        selectorEstado.addEventListener("change", () => { filtroEstado = selectorEstado.value; dibujarContenido(); });

        const contenido = el("div", { class: "gestion-contenido" });
        espacioTrabajo.replaceChildren(barra, contenido);
        dibujarContenido();
    }

    function dibujarContenido() {
        const contenido = espacioTrabajo.querySelector(".gestion-contenido");
        if (!contenido) return;
        const filas = vehiculosFiltrados();
        const tabla = crearTabla({
            titulo: "Vehículos registrados",
            columnas: [
                { clave: "placa", titulo: "Placa" },
                { titulo: "Vehículo", render: (v) => `${v.marca ?? ""} ${v.linea ?? ""}`.trim() || "—" },
                { titulo: "Tipo", render: (v) => textoDe(TIPO_VEHICULO, v.tipoVehiculo) },
                { titulo: "Servicio", render: (v) => textoDe(TIPO_SERVICIO, v.tipoServicio) },
                { titulo: "Documentos", render: (v) => resumenEstados(v.documentos) },
                {
                    titulo: "Acciones", clase: "celda-acciones",
                    render: (v) => el("div", { class: "acciones-tabla" },
                        boton("Detalle", { chico: true, onclick: () => { detalleId = v.id; dibujarContenido(); } }),
                        boton("Editar", { chico: true, onclick: () => mostrarFormularioVehiculo(v) }),
                        boton("Eliminar", { variante: "peligro", chico: true, onclick: () => eliminarVehiculo(v) })),
                },
            ],
            filas,
            vacio: {
                titulo: vehiculos.length ? "No hay coincidencias" : "No hay vehículos registrados",
                texto: vehiculos.length ? "Cambia los filtros o la búsqueda por placa." : "Crea el primer vehículo para empezar a gestionar la flota.",
            },
        });
        contenido.replaceChildren(tabla);
        const seleccionado = vehiculos.find((v) => v.id === detalleId);
        if (seleccionado) contenido.append(crearDetalle(seleccionado));
    }

    function resumenEstados(documentos = []) {
        if (!documentos.length) return etiqueta("Sin documentos", "error");
        const estados = [...new Set(documentos.map((d) => d.estadoDocumento))];
        return el("div", { class: "grupo-etiquetas" }, estados.map((estado) => chipEstadoDocumento(estado)));
    }

    function crearDetalle(vehiculo) {
        const titulo = el("div", { class: "seccion-titulo" },
            el("div", {},
                el("p", { class: "sobretitulo" }, `Vehículo ${vehiculo.id}`),
                el("h2", {}, vehiculo.placa)),
            boton("Cerrar detalle", { chico: true, onclick: () => { detalleId = null; dibujarContenido(); } }));

        const caracteristicas = el("dl", { class: "vehiculo-specs" },
            dato("Marca y línea", `${vehiculo.marca ?? "—"} ${vehiculo.linea ?? ""}`.trim()),
            dato("Modelo", vehiculo.modelo ?? "—"),
            dato("Servicio", textoDe(TIPO_SERVICIO, vehiculo.tipoServicio)),
            dato("Combustible", textoDe(TIPO_COMBUSTIBLE, vehiculo.tipoCombustible)),
            dato("Capacidad", `${vehiculo.capacidadPasajeros ?? "—"} pasajeros`),
            dato("Color", vehiculo.color ?? "—"));

        const documentos = crearTabla({
            titulo: `Documentos del vehículo ${vehiculo.placa}`,
            columnas: [
                { titulo: "Documento", render: (d) => `${d.codigoDocumento ?? ""} · ${d.nombreDocumento ?? ""}` },
                { titulo: "Estado", render: (d) => chipEstadoDocumento(d.estadoDocumento) },
                { titulo: "Vigencia", render: (d) => {
                    const dias = diasHasta(d.fechaVencimiento);
                    return el("span", { class: "vigencia-documento" },
                        `Hasta ${formatearFecha(d.fechaVencimiento)}`,
                        dias === null ? "" : el("small", {}, textoDias(dias)));
                } },
                {
                    titulo: "Acciones", clase: "celda-acciones",
                    render: (d) => el("div", { class: "acciones-tabla" },
                        boton("Cambiar estado / fecha", {
                            chico: true,
                            "aria-expanded": "false",
                            onclick: (evento) => {
                                const accion = evento.currentTarget;
                                const fila = accion.closest("tr");
                                const abierta = fila.nextElementSibling?.classList.contains("fila-edicion-documento");
                                document.querySelectorAll(".fila-edicion-documento").forEach((n) => n.remove());
                                document.querySelectorAll('[aria-expanded="true"]').forEach((n) => n.setAttribute("aria-expanded", "false"));
                                if (!abierta) {
                                    accion.setAttribute("aria-expanded", "true");
                                    fila.after(el("tr", { class: "fila-edicion-documento" },
                                        el("td", { colspan: "4" }, formularioEstado(vehiculo, d))));
                                }
                            },
                        })),
                },
            ],
            filas: vehiculo.documentos ?? [],
            vacio: { titulo: "No hay documentos asociados", texto: "Agrega al menos un documento a este vehículo." },
        });

        const panel = el("section", { class: "vehiculo-detalle tarjeta", "aria-label": `Detalle de ${vehiculo.placa}` },
            titulo, caracteristicas,
            el("div", { class: "seccion-titulo" }, el("h3", {}, "Documentos asociados")),
            documentos,
            el("div", { class: "formulario-acciones acciones-documento" },
                boton("Asociar documento", { variante: "primario", chico: true, onclick: () => mostrarFormularioAsociar(vehiculo) }),
                boton("Subir PDF", { chico: true, onclick: () => mostrarFormularioPdf(vehiculo) })));
        const panelAccion = el("div", { class: "vehiculo-formulario-accion", "aria-live": "polite" });
        panel.append(panelAccion);
        panel.dataset.id = String(vehiculo.id);
        return panel;
    }

    function dato(nombre, valor) {
        return el("div", { class: "vehiculo-spec" }, el("dt", {}, nombre), el("dd", {}, String(valor)));
    }

    function formularioEstado(vehiculo, documento) {
        const estado = crearCampo({
            nombre: "estadoDocumento", etiqueta: "Estado", tipo: "select", requerido: true,
            valor: documento.estadoDocumento, opciones: ESTADOS_DOCUMENTO,
        });
        const fecha = crearCampo({
            nombre: "fechaVencimiento", etiqueta: "Nueva fecha de vencimiento", tipo: "date",
            valor: documento.fechaVencimiento ?? "",
            ayuda: "Si se deja vacía, el servidor conserva la fecha actual.",
        });
        const form = el("form", { class: "formulario formulario-estado-documento" },
            el("div", { class: "formulario-fila" }, estado.contenedor, fecha.contenedor),
            el("div", { class: "formulario-acciones" }, boton("Guardar cambios", { variante: "primario", tipo: "submit" })));
        enviarFormulario(form, {
            textoCargando: "Actualizando…",
            alEnviar: async () => {
                const cuerpo = { estadoDocumento: estado.entrada.value };
                if (fecha.entrada.value) cuerpo.fechaVencimiento = fecha.entrada.value;
                await apiPut(`/api/vehiculos/${vehiculo.id}/documentos/${documento.idDocumento}/estado`, cuerpo);
                avisar("Estado del documento actualizado.", "ok");
                await cargarDatos();
            },
        });
        return form;
    }

    function mostrarFormularioVehiculo(vehiculo = null) {
        if (!catalogo.length) {
            avisar("Primero crea un tipo de documento en el catálogo.", "aviso");
            return;
        }
        espacioTrabajo.querySelector(".formulario-vehiculo")?.remove();
        const editando = Boolean(vehiculo);
        const tipo = crearCampo({ nombre: "tipoVehiculo", etiqueta: "Tipo de vehículo", tipo: "select", requerido: true, valor: vehiculo?.tipoVehiculo ?? "", opciones: TIPOS_VEHICULO });
        const placa = crearCampo({ nombre: "placa", etiqueta: "Placa", requerido: true, valor: vehiculo?.placa ?? "", atributos: { maxlength: "6", minlength: "6", pattern: "[A-Za-z0-9]{6}", title: "Escribe los 6 caracteres de la placa." } });
        const servicio = crearCampo({ nombre: "tipoServicio", etiqueta: "Tipo de servicio", tipo: "select", requerido: true, valor: vehiculo?.tipoServicio ?? "", opciones: TIPOS_SERVICIO });
        const combustible = crearCampo({ nombre: "tipoCombustible", etiqueta: "Combustible", tipo: "select", requerido: true, valor: vehiculo?.tipoCombustible ?? "", opciones: TIPOS_COMBUSTIBLE });
        const capacidad = crearCampo({ nombre: "capacidadPasajeros", etiqueta: "Capacidad de pasajeros", tipo: "number", requerido: true, valor: vehiculo?.capacidadPasajeros ?? "", atributos: { min: "1", step: "1" } });
        const color = crearCampo({ nombre: "color", etiqueta: "Color hexadecimal", requerido: true, valor: vehiculo?.color ?? "#000000", atributos: { pattern: "#[0-9A-Fa-f]{6}", title: "Usa el formato #RRGGBB." } });
        const modelo = crearCampo({ nombre: "modelo", etiqueta: "Año / modelo", tipo: "number", requerido: true, valor: vehiculo?.modelo ?? "", atributos: { step: "1" } });
        const marca = crearCampo({ nombre: "marca", etiqueta: "Marca", requerido: true, valor: vehiculo?.marca ?? "" });
        const linea = crearCampo({ nombre: "linea", etiqueta: "Línea", requerido: true, valor: vehiculo?.linea ?? "" });

        const formulario = el("form", { class: "formulario tarjeta formulario-vehiculo" },
            el("div", { class: "seccion-titulo" },
                el("div", {}, el("p", { class: "sobretitulo" }, editando ? "Actualizar registro" : "Nuevo registro"),
                    el("h2", {}, editando ? "Editar " : "Registrar ", el("em", {}, "vehículo"))),
                boton("Cerrar", { chico: true, onclick: () => formulario.remove() })),
            el("div", { class: "formulario-fila" }, tipo.contenedor, placa.contenedor, servicio.contenedor, combustible.contenedor),
            el("div", { class: "formulario-fila" }, capacidad.contenedor, color.contenedor, modelo.contenedor, marca.contenedor, linea.contenedor),
            editando
                ? documentosEdicion(vehiculo)
                : seccionDocumentosNuevos(catalogo),
            el("div", { class: "formulario-acciones" },
                boton(editando ? "Guardar vehículo" : "Crear vehículo", { variante: "primario", tipo: "submit" }),
                boton("Cancelar", { onclick: () => formulario.remove() })));

        const lista = formulario.querySelector(".documentos-para-vehiculo");
        enviarFormulario(formulario, {
            textoCargando: editando ? "Guardando…" : "Creando…",
            alEnviar: async (form) => {
                let documentos;
                if (editando) {
                    documentos = (vehiculo.documentos ?? []).map((d) => ({
                        idDocumento: d.idDocumento,
                        fechaExpedicion: d.fechaExpedicion,
                        fechaVencimiento: d.fechaVencimiento,
                    }));
                } else {
                    documentos = [...lista.querySelectorAll(".vehiculo-documento-form-row")].map((fila) => ({
                        idDocumento: Number(fila.querySelector('[name="idDocumento"]').value),
                        fechaExpedicion: fila.querySelector('[name="fechaExpedicion"]').value,
                        fechaVencimiento: fila.querySelector('[name="fechaVencimiento"]').value,
                    }));
                }
                if (!documentos.length || documentos.some((d) => !d.idDocumento || !d.fechaExpedicion || !d.fechaVencimiento)) {
                    avisar("Asocia al menos un documento e indica sus fechas.", "error");
                    return;
                }
                const ids = documentos.map((d) => d.idDocumento);
                if (new Set(ids).size !== ids.length) {
                    avisar("No repitas el mismo tipo de documento.", "error");
                    return;
                }
                const cuerpo = {
                    tipoVehiculo: tipo.entrada.value,
                    placa: placa.entrada.value.toUpperCase(),
                    tipoServicio: servicio.entrada.value,
                    tipoCombustible: combustible.entrada.value,
                    capacidadPasajeros: Number(capacidad.entrada.value),
                    color: color.entrada.value,
                    modelo: Number(modelo.entrada.value),
                    marca: marca.entrada.value.trim(),
                    linea: linea.entrada.value.trim(),
                    documentos,
                };
                if (editando) await apiPut(`/api/vehiculos/${vehiculo.id}`, cuerpo);
                else await apiPost("/api/vehiculos", cuerpo);
                avisar(editando ? "Vehículo actualizado." : "Vehículo creado.", "ok");
                detalleId = vehiculo?.id ?? null;
                await cargarDatos();
            },
        });
        espacioTrabajo.querySelector(".gestion-contenido").prepend(formulario);
        formulario.scrollIntoView({ behavior: "smooth", block: "start" });
    }

    function documentosEdicion(vehiculo) {
        const filas = (vehiculo.documentos ?? []).map((d) =>
            el("div", { class: "vehiculo-documento-resumen" },
                el("span", {}, `${d.codigoDocumento} · ${d.nombreDocumento}`),
                el("span", {}, `Expide ${formatearFecha(d.fechaExpedicion)} · Vence ${formatearFecha(d.fechaVencimiento)}`),
                el("input", { type: "hidden", name: "idDocumento", value: d.idDocumento })));
        return el("section", { class: "documentos-para-vehiculo" },
            el("h3", {}, "Documentos existentes"),
            el("p", { class: "campo-ayuda" }, "La edición conserva las asociaciones actuales. Usa el detalle para agregar o actualizar documentos."),
            filas.length ? filas : el("p", { class: "mensaje" }, "Este vehículo no tiene documentos asociados."));
    }

    function seccionDocumentosNuevos(documentosCatalogo) {
        const filas = el("div", { class: "vehiculo-documentos-form-filas" });
        const agregarFila = (documento = null) => {
            const selector = crearCampo({
                nombre: "idDocumento", etiqueta: "Documento", tipo: "select", requerido: true,
                valor: documento?.idDocumento ?? "",
                opciones: documentosCatalogo.map((d) => ({ valor: d.id, texto: `${d.codigoDocumento} · ${d.nombreDocumento}` })),
            });
            const expedicion = crearCampo({ nombre: "fechaExpedicion", etiqueta: "Fecha de expedición", tipo: "date", requerido: true, valor: documento?.fechaExpedicion ?? "" });
            const vencimiento = crearCampo({ nombre: "fechaVencimiento", etiqueta: "Fecha de vencimiento", tipo: "date", requerido: true, valor: documento?.fechaVencimiento ?? "" });
            const fila = el("div", { class: "vehiculo-documento-form-row" },
                selector.contenedor, expedicion.contenedor, vencimiento.contenedor);
            if (filas.children.length > 0) {
                fila.append(boton("Quitar", {
                    variante: "peligro", chico: true,
                    "aria-label": "Quitar documento",
                    onclick: () => fila.remove(),
                }));
            }
            filas.append(fila);
        };
        agregarFila();
        return el("section", { class: "documentos-para-vehiculo" },
            el("div", { class: "seccion-titulo" },
                el("div", {}, el("h3", {}, "Documentos iniciales"), el("p", { class: "campo-ayuda" }, "Se requiere al menos uno.")),
                boton("Agregar documento", { chico: true, onclick: () => agregarFila() })),
            filas);
    }

    async function eliminarVehiculo(vehiculo) {
        const aceptar = await confirmar({
            titulo: `¿Eliminar el vehículo ${vehiculo.placa}?`,
            texto: "Esta acción elimina el registro del vehículo. Si tiene información relacionada, el servidor indicará si no es posible continuar.",
            textoConfirmar: "Eliminar vehículo",
            peligro: true,
        });
        if (!aceptar) return;
        try {
            await apiDelete(`/api/vehiculos/${vehiculo.id}`);
            if (detalleId === vehiculo.id) detalleId = null;
            avisar("Vehículo eliminado.", "ok");
            await cargarDatos();
        } catch (error) {
            mostrarError(error);
        }
    }

    function mostrarFormularioAsociar(vehiculo) {
        const panel = espacioTrabajo.querySelector(".vehiculo-formulario-accion");
        const asociados = new Set((vehiculo.documentos ?? []).map((d) => d.idDocumento));
        const disponibles = catalogo.filter((d) => !asociados.has(d.id));
        if (!disponibles.length) {
            panel.replaceChildren(el("p", { class: "mensaje" }, "Todos los documentos del catálogo ya están asociados."));
            return;
        }
        const documento = crearCampo({
            nombre: "idDocumento", etiqueta: "Documento", tipo: "select", requerido: true,
            opciones: disponibles.map((d) => ({ valor: d.id, texto: `${d.codigoDocumento} · ${d.nombreDocumento}` })),
        });
        const expedicion = crearCampo({ nombre: "fechaExpedicion", etiqueta: "Fecha de expedición", tipo: "date", requerido: true });
        const vencimiento = crearCampo({ nombre: "fechaVencimiento", etiqueta: "Fecha de vencimiento", tipo: "date", requerido: true });
        const form = el("form", { class: "formulario tarjeta" },
            el("h3", {}, "Asociar documento"),
            el("div", { class: "formulario-fila" }, documento.contenedor, expedicion.contenedor, vencimiento.contenedor),
            el("div", { class: "formulario-acciones" }, boton("Asociar", { variante: "primario", tipo: "submit" }),
                boton("Cancelar", { onclick: () => panel.replaceChildren() })));
        enviarFormulario(form, {
            textoCargando: "Asociando…",
            alEnviar: async () => {
                await apiPost(`/api/vehiculos/${vehiculo.id}/documentos`, {
                    idDocumento: Number(documento.entrada.value),
                    fechaExpedicion: expedicion.entrada.value,
                    fechaVencimiento: vencimiento.entrada.value,
                });
                avisar("Documento asociado. Inicia en estado En verificación.", "ok");
                detalleId = vehiculo.id;
                await cargarDatos();
            },
        });
        panel.replaceChildren(form);
    }

    function mostrarFormularioPdf(vehiculo) {
        const panel = espacioTrabajo.querySelector(".vehiculo-formulario-accion");
        const documentos = vehiculo.documentos ?? [];
        if (!documentos.length) {
            panel.replaceChildren(el("p", { class: "mensaje" }, "Asocia un documento antes de cargar su PDF."));
            return;
        }
        const selector = crearCampo({
            nombre: "idDocumento", etiqueta: "Documento asociado", tipo: "select", requerido: true,
            opciones: documentos.map((d) => ({ valor: d.idDocumento, texto: `${d.codigoDocumento} · ${d.nombreDocumento}` })),
        });
        const archivo = crearCampo({
            nombre: "archivo", etiqueta: "Archivo PDF", tipo: "file", requerido: true,
            ayuda: "Solo PDF; tamaño máximo 2 MB.",
            atributos: { accept: "application/pdf,.pdf" },
        });
        const form = el("form", { class: "formulario tarjeta" },
            el("h3", {}, "Cargar archivo del documento"),
            el("div", { class: "formulario-fila" }, selector.contenedor, archivo.contenedor),
            el("div", { class: "formulario-acciones" }, boton("Subir PDF", { variante: "primario", tipo: "submit" }),
                boton("Cancelar", { onclick: () => panel.replaceChildren() })));
        enviarFormulario(form, {
            textoCargando: "Subiendo…",
            alEnviar: async () => {
                const file = archivo.entrada.files[0];
                if (!file || (!file.type.includes("pdf") && !file.name.toLowerCase().endsWith(".pdf"))) {
                    mostrarErrorCampo(archivo.entrada, "Selecciona un archivo PDF.");
                    return;
                }
                if (file.size > TAMANO_MAX_PDF) {
                    mostrarErrorCampo(archivo.entrada, "El archivo supera el límite de 2 MB.");
                    return;
                }
                const base64 = await leerBase64(file);
                await apiPost(`/api/vehiculos/${vehiculo.id}/documentos/pdf`, [{
                    idDocumento: Number(selector.entrada.value),
                    documentoBase64: base64,
                }]);
                avisar("PDF cargado correctamente.", "ok");
                await cargarDatos();
            },
        });
        panel.replaceChildren(form);
    }
}

function leerBase64(archivo) {
    return new Promise((resolver, rechazar) => {
        const lector = new FileReader();
        lector.addEventListener("load", () => {
            const resultado = String(lector.result ?? "");
            const separador = resultado.indexOf(",");
            if (separador < 0) {
                rechazar(new Error("No se pudo leer el archivo PDF."));
                return;
            }
            resolver(resultado.slice(separador + 1));
        });
        lector.addEventListener("error", () => rechazar(lector.error ?? new Error("No se pudo leer el archivo PDF.")));
        lector.readAsDataURL(archivo);
    });
}

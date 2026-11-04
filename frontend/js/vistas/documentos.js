// ============================================================================
// vistas/documentos.js · CATÁLOGO DE TIPOS DE DOCUMENTO
// ============================================================================
// Operaciones del catálogo existente en DocumentoController: GET/POST /api/documentos
// y GET/PUT/DELETE /api/documentos/{id}. Los códigos de aplicación y obligatoriedad
// corresponden a los enums del backend (A/M/AM y RA/RM/RR).

import { apiDelete, apiGet, apiPost, apiPut } from "../api.js";
import { confirmar } from "../ui/dialogo.js";
import { crearCampo, enviarFormulario } from "../ui/formulario.js";
import { avisar, estadoCarga, estadoError, mostrarError } from "../ui/mensajes.js";
import { crearTabla } from "../ui/tabla.js";
import { boton, el, encabezadoVista } from "../ui/util.js";

const APLICA_VEHICULO = [
    { valor: "A", texto: "Automóvil" },
    { valor: "M", texto: "Motocicleta" },
    { valor: "AM", texto: "Ambos" },
];
const OBLIGATORIEDAD = [
    { valor: "RA", texto: "Requerido en automóvil" },
    { valor: "RM", texto: "Requerido en motocicleta" },
    { valor: "RR", texto: "Requerido en ambos" },
];

export async function montar(contenedor) {
    contenedor.append(encabezadoVista({
        sobretitulo: "Parte 3 · Configuración",
        antes: "Catálogo de ",
        destacado: "documentos",
        descripcion: "Define los documentos admitidos y en qué tipos de vehículo se exigen.",
    }));
    const contenido = el("div", { class: "gestion-layout" }, estadoCarga("Cargando catálogo…"));
    contenedor.append(contenido);
    await cargar();

    async function cargar() {
        contenido.replaceChildren(estadoCarga("Cargando catálogo…"));
        try {
            const documentos = await apiGet("/api/documentos");
            dibujar(documentos);
        } catch (error) {
            contenido.replaceChildren(estadoError(error.message, cargar));
        }
    }

    function dibujar(documentos) {
        const accion = el("div", { class: "gestion-barra gestion-barra--catalogo" },
            el("p", { class: "mensaje" }, `${documentos.length} tipo${documentos.length === 1 ? "" : "s"} de documento registrado${documentos.length === 1 ? "" : "s"}.`),
            boton("Nuevo tipo de documento", { variante: "primario", onclick: () => mostrarFormulario() }));
        const tabla = crearTabla({
            titulo: "Catálogo de documentos",
            columnas: [
                { clave: "codigoDocumento", titulo: "Código" },
                { clave: "nombreDocumento", titulo: "Nombre" },
                { titulo: "Aplica a", render: (d) => textoAplica(d.tipoVehiculoAplica) },
                { titulo: "Obligatoriedad", render: (d) => textoObligatoriedad(d.obligatoriedad) },
                { clave: "descripcion", titulo: "Descripción" },
                {
                    titulo: "Acciones", clase: "celda-acciones",
                    render: (d) => el("div", { class: "acciones-tabla" },
                        boton("Editar", { chico: true, onclick: () => mostrarFormulario(d) }),
                        boton("Eliminar", { variante: "peligro", chico: true, onclick: () => eliminar(d) })),
                },
            ],
            filas: documentos,
            vacio: { titulo: "El catálogo está vacío", texto: "Agrega tipos de documento para poder asociarlos a vehículos." },
        });
        contenido.replaceChildren(accion, tabla);
    }

    function mostrarFormulario(documento = null) {
        contenido.querySelector(".formulario-catalogo")?.remove();
        const editando = Boolean(documento);
        const codigo = crearCampo({
            nombre: "codigoDocumento", etiqueta: "Código", requerido: true,
            valor: documento?.codigoDocumento ?? "",
            atributos: { maxlength: "20" },
            ayuda: "Código corto único, por ejemplo SOAT.",
        });
        const nombre = crearCampo({
            nombre: "nombreDocumento", etiqueta: "Nombre", requerido: true,
            valor: documento?.nombreDocumento ?? "",
            atributos: { maxlength: "100" },
        });
        const aplica = crearCampo({
            nombre: "tipoVehiculoAplica", etiqueta: "Tipo de vehículo al que aplica", tipo: "select",
            requerido: true, valor: documento?.tipoVehiculoAplica ?? "", opciones: APLICA_VEHICULO,
        });
        const obligatoriedad = crearCampo({
            nombre: "obligatoriedad", etiqueta: "Obligatoriedad", tipo: "select",
            requerido: true, valor: documento?.obligatoriedad ?? "", opciones: OBLIGATORIEDAD,
        });
        const descripcion = crearCampo({
            nombre: "descripcion", etiqueta: "Descripción", tipo: "textarea",
            valor: documento?.descripcion ?? "", atributos: { maxlength: "255" },
        });
        const form = el("form", { class: "formulario tarjeta formulario-catalogo" },
            el("div", { class: "seccion-titulo" },
                el("div", {}, el("p", { class: "sobretitulo" }, editando ? "Editar registro" : "Nuevo registro"),
                    el("h2", {}, editando ? "Editar tipo de " : "Nuevo tipo de ", el("em", {}, "documento"))),
                boton("Cerrar", { chico: true, onclick: () => form.remove() })),
            el("div", { class: "formulario-fila" }, codigo.contenedor, nombre.contenedor, aplica.contenedor, obligatoriedad.contenedor),
            descripcion.contenedor,
            el("div", { class: "formulario-acciones" },
                boton(editando ? "Guardar cambios" : "Crear documento", { variante: "primario", tipo: "submit" }),
                boton("Cancelar", { onclick: () => form.remove() })));
        enviarFormulario(form, {
            textoCargando: "Guardando…",
            alEnviar: async () => {
                const cuerpo = {
                    codigoDocumento: codigo.entrada.value.trim(),
                    nombreDocumento: nombre.entrada.value.trim(),
                    tipoVehiculoAplica: aplica.entrada.value,
                    obligatoriedad: obligatoriedad.entrada.value,
                    descripcion: descripcion.entrada.value.trim() || null,
                };
                if (editando) await apiPut(`/api/documentos/${documento.id}`, cuerpo);
                else await apiPost("/api/documentos", cuerpo);
                avisar(editando ? "Tipo de documento actualizado." : "Tipo de documento creado.", "ok");
                await cargar();
            },
        });
        contenido.prepend(form);
        form.scrollIntoView({ behavior: "smooth", block: "start" });
    }

    async function eliminar(documento) {
        const aceptar = await confirmar({
            titulo: `¿Eliminar ${documento.nombreDocumento}?`,
            texto: "Si este tipo está asociado a vehículos, el servidor impedirá eliminarlo y mostrará el motivo.",
            textoConfirmar: "Eliminar documento",
            peligro: true,
        });
        if (!aceptar) return;
        try {
            await apiDelete(`/api/documentos/${documento.id}`);
            avisar("Tipo de documento eliminado.", "ok");
            await cargar();
        } catch (error) {
            mostrarError(error);
        }
    }
}

function textoAplica(codigo) {
    return APLICA_VEHICULO.find((opcion) => opcion.valor === codigo)?.texto ?? codigo ?? "—";
}

function textoObligatoriedad(codigo) {
    return OBLIGATORIEDAD.find((opcion) => opcion.valor === codigo)?.texto ?? codigo ?? "—";
}

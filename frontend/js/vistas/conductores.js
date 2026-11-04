// ============================================================================
// vistas/conductores.js · ASOCIACIONES DE CONDUCTORES POR VEHÍCULO
// ============================================================================
// Usa las rutas existentes de VehiculoPersonaController. Las nuevas relaciones
// empiezan en EA por regla del servidor; aquí solo se eligen estados válidos.

import { apiGet, apiPost, apiPut } from "../api.js";
import { chipEstadoConductor } from "../ui/catalogos.js";
import { crearCampo, enviarFormulario } from "../ui/formulario.js";
import { avisar, estadoCarga, estadoError, mostrarError } from "../ui/mensajes.js";
import { crearTabla } from "../ui/tabla.js";
import { boton, el, encabezadoVista, formatearFecha } from "../ui/util.js";

const ESTADOS = [
    { valor: "PO", texto: "Puede operar" },
    { valor: "EA", texto: "En espera de aprobación" },
    { valor: "RO", texto: "Restringido" },
];

export async function montar(contenedor) {
    let vehiculos = [];
    let conductores = [];
    let asociaciones = [];
    let idVehiculo = "";

    contenedor.append(encabezadoVista({
        sobretitulo: "Parte 4 · Asignaciones",
        antes: "Conductores por ",
        destacado: "vehículo",
        descripcion: "Asocia conductores a la flota y administra su estado operativo por vehículo.",
    }));
    const espacioTrabajo = el("div", { class: "gestion-layout" }, estadoCarga("Cargando vehículos…"));
    contenedor.append(espacioTrabajo);
    await cargarCatalogos();

    async function cargarCatalogos() {
        try {
            [vehiculos, conductores] = await Promise.all([
                apiGet("/api/vehiculos"),
                apiGet("/api/personas"),
            ]);
            conductores = conductores.filter((p) => p.tipoPersona === "C");
            dibujar();
            if (idVehiculo) await cargarAsociaciones();
        } catch (error) {
            espacioTrabajo.replaceChildren(estadoError(error.message, cargarCatalogos));
        }
    }

    function dibujar() {
        const selector = crearCampo({
            nombre: "vehiculo", etiqueta: "Selecciona un vehículo", tipo: "select",
            requerido: true,
            valor: idVehiculo,
            opciones: vehiculos.map((v) => ({ valor: v.id, texto: `${v.placa} · ${v.marca} ${v.linea}` })),
        });
        selector.entrada.addEventListener("change", async () => {
            idVehiculo = selector.entrada.value;
            await cargarAsociaciones();
        });
        const barra = el("section", { class: "gestion-barra gestion-barra--conductores tarjeta" },
            selector.contenedor,
            el("p", { class: "mensaje", role: "status" },
                idVehiculo ? "Las nuevas asociaciones quedan en espera de aprobación (EA)." : "Elige un vehículo para consultar sus conductores."));
        const contenido = el("div", { class: "gestion-contenido" });
        espacioTrabajo.replaceChildren(barra, contenido);
        if (idVehiculo) dibujarAsociaciones();
        else contenido.replaceChildren(el("div", { class: "estado" },
            el("h3", {}, "Selecciona un vehículo"),
            el("p", {}, "La lista mostrará las personas asociadas y su estado para ese vehículo.")));
    }

    async function cargarAsociaciones() {
        if (!idVehiculo) {
            dibujar();
            return;
        }
        const contenido = espacioTrabajo.querySelector(".gestion-contenido");
        if (contenido) contenido.replaceChildren(estadoCarga("Cargando conductores asociados…"));
        try {
            asociaciones = await apiGet(`/api/vehiculos/${idVehiculo}/conductores`);
            dibujar();
        } catch (error) {
            if (contenido?.isConnected) contenido.replaceChildren(estadoError(error.message, cargarAsociaciones));
        }
    }

    function dibujarAsociaciones() {
        const contenido = espacioTrabajo.querySelector(".gestion-contenido");
        if (!contenido) return;
        const idsAsociados = new Set(asociaciones.map((a) => idPersona(a)));
        const disponibles = conductores.filter((p) => !idsAsociados.has(Number(p.idPersona)));
        const selector = crearCampo({
            nombre: "persona", etiqueta: "Conductor", tipo: "select", requerido: true,
            opciones: disponibles.map((p) => ({
                valor: p.idPersona,
                texto: `${p.nombres} ${p.apellidos} · ${p.identificacion}`,
            })),
        });
        const fecha = crearCampo({
            nombre: "fechaAsociacion", etiqueta: "Fecha de asociación", tipo: "date",
            requerido: true, valor: fechaActualLocal(),
        });
        const formulario = el("form", { class: "formulario tarjeta formulario-asociacion" },
            el("div", { class: "seccion-titulo" },
                el("div", {}, el("h3", {}, "Asociar un conductor"),
                    el("p", { class: "campo-ayuda" }, "Al asociar, el estado inicial será En espera de aprobación (EA).")),
                null),
            disponibles.length
                ? el("div", { class: "formulario-fila" }, selector.contenedor, fecha.contenedor)
                : el("p", { class: "mensaje" }, conductores.length
                    ? "Todos los conductores registrados ya están asociados a este vehículo."
                    : "No hay conductores registrados. Crea una persona de tipo conductor primero."),
            disponibles.length
                ? el("div", { class: "formulario-acciones" }, boton("Asociar conductor", { variante: "primario", tipo: "submit" }))
                : null);
        if (disponibles.length) {
            enviarFormulario(formulario, {
                textoCargando: "Asociando…",
                alEnviar: async () => {
                    await apiPost(`/api/vehiculos/${idVehiculo}/conductores`, {
                        idPersona: Number(selector.entrada.value),
                        fechaAsociacion: fecha.entrada.value,
                    });
                    avisar("Conductor asociado en estado En espera de aprobación (EA).", "ok");
                    await cargarAsociaciones();
                },
            });
        }

        const tabla = crearTabla({
            titulo: "Conductores asociados al vehículo",
            columnas: [
                { titulo: "Conductor", render: (a) => {
                    const p = a.persona ?? {};
                    return `${p.nombres ?? ""} ${p.apellidos ?? ""}`.trim() || `Persona ${idPersona(a)}`;
                } },
                { titulo: "Identificación", render: (a) => a.persona?.identificacion ?? "—" },
                { titulo: "Licencia", render: (a) => a.persona?.fechaVigenciaLicencia
                    ? `Hasta ${formatearFecha(a.persona.fechaVigenciaLicencia)}`
                    : "Sin fecha registrada" },
                { titulo: "Asociado desde", render: (a) => formatearFecha(a.fechaAsociacion) },
                { titulo: "Estado", render: (a) => chipEstadoConductor(a.estadoConductor) },
                {
                    titulo: "Cambiar estado", clase: "celda-acciones",
                    render: (a) => selectorEstado(a),
                },
            ],
            filas: asociaciones,
            vacio: {
                titulo: "Aún no hay conductores asociados",
                texto: "Selecciona un conductor en el formulario para crear la primera asociación.",
            },
        });
        contenido.replaceChildren(formulario, tabla);
    }

    function selectorEstado(asociacion) {
        const select = el("select", {
            class: "selector-estado-conductor",
            "aria-label": `Estado de ${asociacion.persona?.nombres ?? "conductor"} ${asociacion.persona?.apellidos ?? ""}`,
        }, ESTADOS.map((estado) => el("option", { value: estado.valor }, estado.texto)));
        select.value = asociacion.estadoConductor;
        select.addEventListener("change", async () => {
            const estadoAnterior = asociacion.estadoConductor;
            select.disabled = true;
            try {
                await apiPut(`/api/vehiculos/${idVehiculo}/conductores/${idPersona(asociacion)}/estado`, {
                    estadoConductor: select.value,
                });
                avisar("Estado del conductor actualizado.", "ok");
                await cargarAsociaciones();
            } catch (error) {
                select.value = estadoAnterior;
                mostrarError(error);
            } finally {
                if (select.isConnected) select.disabled = false;
            }
        });
        return select;
    }
}

function idPersona(asociacion) {
    return Number(asociacion.persona?.idPersona ?? asociacion.id?.idPersona);
}

function fechaActualLocal() {
    const hoy = new Date();
    return `${hoy.getFullYear()}-${String(hoy.getMonth() + 1).padStart(2, "0")}-${String(hoy.getDate()).padStart(2, "0")}`;
}

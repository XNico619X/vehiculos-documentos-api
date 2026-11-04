// ============================================================================
// vistas/personas.js · PERSONAS, LICENCIAS Y CUENTAS ADMINISTRATIVAS
// ============================================================================
// La licencia solo se envía al crear o reemplazar un PDF. Al editar, el
// backend conserva la licencia existente si no se manda un archivo nuevo.

import { apiGet, apiPost, apiPut } from "../api.js";
import { mostrarDialogo } from "../ui/dialogo.js";
import { crearCampo, enviarFormulario, mostrarErrorCampo } from "../ui/formulario.js";
import { avisar, estadoCarga, estadoError } from "../ui/mensajes.js";
import { crearTabla } from "../ui/tabla.js";
import { boton, el, encabezadoVista, formatearFecha } from "../ui/util.js";

const TIPOS_PERSONA = [
    { valor: "C", texto: "Conductor" },
    { valor: "A", texto: "Administrativo" },
];
const TIPOS_IDENTIFICACION = [{ valor: "CC", texto: "Cédula de ciudadanía" }];
let contadorCredenciales = 0;

export async function montar(contenedor) {
    let personas = [];
    let termino = "";
    let tipo = "";

    contenedor.append(encabezadoVista({
        sobretitulo: "Parte 4 · Registro",
        antes: "Personas y ",
        destacado: "conductores",
        descripcion: "Administra la información personal, las licencias y las cuentas administrativas.",
    }));
    const espacioTrabajo = el("div", { class: "gestion-layout" });
    contenedor.append(espacioTrabajo);
    await cargar();

    async function cargar() {
        espacioTrabajo.replaceChildren(estadoCarga("Cargando personas…"));
        try {
            personas = await apiGet("/api/personas");
            dibujar();
        } catch (error) {
            espacioTrabajo.replaceChildren(estadoError(error.message, cargar));
        }
    }

    function dibujar() {
        const barra = el("section", { class: "gestion-barra gestion-barra--personas tarjeta", "aria-label": "Búsqueda y filtros de personas" },
            crearCampo({
                nombre: "buscarPersona", etiqueta: "Buscar", tipo: "search",
                placeholder: "Nombre o identificación", valor: termino,
            }).contenedor,
            crearCampo({
                nombre: "tipoPersonaFiltro", etiqueta: "Tipo de persona", tipo: "select",
                valor: tipo, opciones: TIPOS_PERSONA,
            }).contenedor,
            boton("Nueva persona", { variante: "primario", onclick: () => mostrarFormulario() }));
        const busqueda = barra.querySelector('[name="buscarPersona"]');
        const selectorTipo = barra.querySelector('[name="tipoPersonaFiltro"]');
        busqueda.addEventListener("input", () => { termino = busqueda.value; dibujarTabla(); });
        selectorTipo.addEventListener("change", () => { tipo = selectorTipo.value; dibujarTabla(); });

        const lista = el("div", { class: "gestion-contenido" });
        espacioTrabajo.replaceChildren(barra, lista);
        dibujarTabla();
    }

    function dibujarTabla() {
        const lista = espacioTrabajo.querySelector(".gestion-contenido");
        if (!lista) return;
        const texto = termino.trim().toLocaleLowerCase("es-CO");
        const filtradas = personas.filter((persona) => {
            const nombre = `${persona.nombres ?? ""} ${persona.apellidos ?? ""}`.toLocaleLowerCase("es-CO");
            const coincideTexto = !texto || nombre.includes(texto) || String(persona.identificacion ?? "").toLocaleLowerCase("es-CO").includes(texto);
            return coincideTexto && (!tipo || persona.tipoPersona === tipo);
        });
        lista.replaceChildren(crearTabla({
            titulo: "Personas registradas",
            columnas: [
                { clave: "identificacion", titulo: "Identificación" },
                { titulo: "Nombre", render: (p) => `${p.nombres ?? ""} ${p.apellidos ?? ""}`.trim() },
                { clave: "correoElectronico", titulo: "Correo" },
                { titulo: "Tipo", render: (p) => p.tipoPersona === "C" ? "Conductor" : "Administrativo" },
                {
                    titulo: "Licencia", render: (p) => p.tipoPersona !== "C"
                        ? "No aplica"
                        : p.tieneLicencia
                            ? `Vigente hasta ${formatearFecha(p.fechaVigenciaLicencia)}`
                            : "Sin archivo registrado",
                },
                {
                    titulo: "Acciones", clase: "celda-acciones",
                    render: (p) => boton("Editar", { chico: true, onclick: () => mostrarFormulario(p) }),
                },
            ],
            filas: filtradas,
            vacio: {
                titulo: personas.length ? "No hay coincidencias" : "No hay personas registradas",
                texto: personas.length ? "Cambia los filtros o el texto de búsqueda." : "Registra una persona para comenzar.",
            },
        }));
    }

    function mostrarFormulario(persona = null) {
        espacioTrabajo.querySelector(".formulario-persona")?.remove();
        const editando = Boolean(persona);
        const identificacion = crearCampo({
            nombre: "identificacion", etiqueta: "Identificación", requerido: true,
            valor: persona?.identificacion ?? "",
        });
        const tipoIdentificacion = crearCampo({
            nombre: "tipoIdentificacion", etiqueta: "Tipo de identificación", tipo: "select",
            requerido: true, valor: persona?.tipoIdentificacion ?? "", opciones: TIPOS_IDENTIFICACION,
        });
        const nombres = crearCampo({
            nombre: "nombres", etiqueta: "Nombres", requerido: true, valor: persona?.nombres ?? "",
        });
        const apellidos = crearCampo({
            nombre: "apellidos", etiqueta: "Apellidos", requerido: true, valor: persona?.apellidos ?? "",
        });
        const correo = crearCampo({
            nombre: "correoElectronico", etiqueta: "Correo electrónico", tipo: "email",
            requerido: true, valor: persona?.correoElectronico ?? "",
            atributos: { autocomplete: "email" },
        });
        const tipoPersona = crearCampo({
            nombre: "tipoPersona", etiqueta: "Tipo de persona", tipo: "select",
            requerido: true, valor: persona?.tipoPersona ?? "", opciones: TIPOS_PERSONA,
        });
        const fechaLicencia = crearCampo({
            nombre: "fechaVigenciaLicencia", etiqueta: "Vigencia de la licencia", tipo: "date",
            requerido: true, valor: persona?.fechaVigenciaLicencia ?? "",
        });
        const archivoLicencia = crearCampo({
            nombre: "licencia", etiqueta: editando && persona.tieneLicencia ? "Reemplazar licencia (opcional)" : "Licencia de conducción (PDF)",
            tipo: "file", requerido: !editando || !persona.tieneLicencia,
            ayuda: editando && persona.tieneLicencia
                ? "No selecciones un archivo para conservar la licencia actual."
                : "Archivo PDF requerido para registrar un conductor.",
            atributos: { accept: "application/pdf,.pdf" },
        });
        const seccionLicencia = el("div", { class: "seccion-licencia" },
            el("div", { class: "formulario-fila" }, fechaLicencia.contenedor, archivoLicencia.contenedor));
        const formulario = el("form", { class: "formulario tarjeta formulario-persona" },
            el("div", { class: "seccion-titulo" },
                el("div", {},
                    el("p", { class: "sobretitulo" }, editando ? "Actualizar registro" : "Nuevo registro"),
                    el("h2", {}, editando ? "Editar " : "Registrar ", el("em", {}, "persona"))),
                boton("Cerrar", { chico: true, onclick: () => formulario.remove() })),
            el("div", { class: "formulario-fila" },
                identificacion.contenedor, tipoIdentificacion.contenedor, nombres.contenedor,
                apellidos.contenedor, correo.contenedor, tipoPersona.contenedor),
            seccionLicencia,
            el("div", { class: "formulario-acciones" },
                boton(editando ? "Guardar cambios" : "Crear persona", { variante: "primario", tipo: "submit" }),
                boton("Cancelar", { onclick: () => formulario.remove() })));

        const actualizarSeccionLicencia = () => {
            const esConductor = tipoPersona.entrada.value === "C";
            seccionLicencia.hidden = !esConductor;
            fechaLicencia.entrada.required = esConductor;
            archivoLicencia.entrada.required = esConductor && (!editando || !persona.tieneLicencia);
        };
        tipoPersona.entrada.addEventListener("change", actualizarSeccionLicencia);
        actualizarSeccionLicencia();

        enviarFormulario(formulario, {
            textoCargando: editando ? "Guardando…" : "Creando…",
            alEnviar: async () => {
                const tipoElegido = tipoPersona.entrada.value;
                const archivo = archivoLicencia.entrada.files[0];
                if (tipoElegido === "C" && !archivo && !editando && !persona?.tieneLicencia) {
                    mostrarErrorCampo(archivoLicencia.entrada, "Adjunta el PDF de la licencia del conductor.");
                    return;
                }
                const cuerpo = {
                    identificacion: identificacion.entrada.value.trim(),
                    tipoIdentificacion: tipoIdentificacion.entrada.value,
                    nombres: nombres.entrada.value.trim(),
                    apellidos: apellidos.entrada.value.trim(),
                    correoElectronico: correo.entrada.value.trim(),
                    tipoPersona: tipoElegido,
                };
                if (tipoElegido === "C") {
                    if (fechaLicencia.entrada.value) cuerpo.fechaVigenciaLicencia = fechaLicencia.entrada.value;
                    if (archivo) {
                        const base64 = await leerBase64(archivo, archivoLicencia.entrada);
                        if (!base64) return;
                        cuerpo.licenciaConduccionBase64 = base64;
                    }
                }
                const respuesta = editando
                    ? await apiPut(`/api/personas/${persona.idPersona}`, cuerpo)
                    : await apiPost("/api/personas", cuerpo);
                avisar(editando ? "Persona actualizada." : "Persona creada.", "ok");
                formulario.remove();
                await cargar();
                if (respuesta?.usuario?.password && respuesta.usuario.apikey) {
                    await mostrarCredenciales(respuesta.usuario);
                }
            },
        });
        espacioTrabajo.querySelector(".gestion-contenido").prepend(formulario);
        formulario.scrollIntoView({ behavior: "smooth", block: "start" });
    }
}

async function leerBase64(archivo, entrada) {
    if ((!archivo.type.includes("pdf") && !archivo.name.toLowerCase().endsWith(".pdf"))) {
        mostrarErrorCampo(entrada, "Selecciona un archivo PDF.");
        return null;
    }
    return new Promise((resolver, rechazar) => {
        const lector = new FileReader();
        lector.addEventListener("load", () => {
            const resultado = String(lector.result ?? "");
            const separador = resultado.indexOf(",");
            if (separador < 0) {
                rechazar(new Error("No se pudo leer el archivo PDF."));
                return;
            }
            const base64 = resultado.slice(separador + 1);
            if (!base64.startsWith("JVBERi0")) {
                mostrarErrorCampo(entrada, "El archivo no contiene un PDF válido.");
                resolver(null);
                return;
            }
            resolver(base64);
        });
        lector.addEventListener("error", () => rechazar(lector.error ?? new Error("No se pudo leer el archivo PDF.")));
        lector.readAsDataURL(archivo);
    });
}

async function mostrarCredenciales(usuario) {
    const contenido = el("div", { class: "credenciales-admin" },
        el("p", {}, "Guarda estos datos ahora. La contraseña y la API Key no se volverán a mostrar."),
        filaCredencial("Usuario", usuario.login),
        filaCredencial("Contraseña", usuario.password),
        filaCredencial("API Key", usuario.apikey));
    const confirmacion = el("label", { class: "confirmacion-credenciales" },
        el("input", { type: "checkbox", name: "credencialesGuardadas" }),
        el("span", {}, "Ya guardé las credenciales en un lugar seguro."));
    contenido.append(confirmacion);
    await mostrarDialogo({
        titulo: "Credenciales del administrador",
        contenido,
        cancelable: false,
        acciones: [{ texto: "Continuar", valor: "guardadas", variante: "primario" }],
        alMontar: (_dialogo, botones) => {
            const continuar = botones[0];
            continuar.disabled = true;
            confirmacion.querySelector("input").addEventListener("change", (evento) => {
                continuar.disabled = !evento.currentTarget.checked;
            });
        },
    });
}

function filaCredencial(nombre, valor) {
    const identificador = `credencial-${nombre.toLowerCase().replace(/\s+/g, "-")}-${++contadorCredenciales}`;
    const entrada = el("input", {
        id: identificador, value: valor, readonly: true,
        "aria-label": nombre,
    });
    const botonCopiar = boton("Copiar", {
        chico: true,
        onclick: async () => {
            entrada.focus();
            entrada.select();
            try {
                await navigator.clipboard.writeText(valor);
                avisar(`${nombre} copiado.`, "ok");
            } catch {
                avisar(`No se pudo copiar automáticamente. Selecciona y copia el valor de ${nombre}.`, "aviso");
            }
        },
    });
    return el("div", { class: "credencial-fila" },
        el("label", { for: identificador }, nombre),
        entrada,
        botonCopiar);
}

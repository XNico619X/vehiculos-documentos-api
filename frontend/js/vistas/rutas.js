// ============================================================================
// vistas/rutas.js · RUTAS Y TRAYECTOS  (#/rutas)
// ============================================================================
// Dos partes en la misma pantalla:
//
// 1. NUEVA RUTA  ->  POST /api/trayectos/rutas   (una lista: una entrada por parada)
//    · se elige el vehículo (GET /api/vehiculos) y aparecen SOLO sus conductores en PO
//      (GET /api/vehiculos/{id}/conductores)
//    · paradas dinámicas: mínimo 2, máximo 7, con Agregar / Quitar / Subir / Bajar
//    · ordenParada NO se escribe: es la posición en la lista (0, 1, 2…)
//    · el servidor sigue siendo quien valida (vehículo habilitado, código repetido...) y su mensaje
//      se muestra tal cual. Al crear, se abre el mapa de la ruta.
//
// 2. CONSULTAS
//    · por identificación del conductor -> GET /api/trayectos/conductores/{identificacion}/rutas  -> ["COD1", ...]
//    · por placa                        -> GET /api/trayectos/vehiculos/{placa}/rutas            -> [{ codigoRuta, conductor, totalParadas }]

import { apiGet, apiPost, SesionExpiradaError } from "../api.js";
import { el, boton, etiqueta, encabezadoVista } from "../ui/util.js";
import { crearCampo, enviarFormulario, mostrarErrorCampo, mostrarErrorGeneral } from "../ui/formulario.js";
import { avisar, anunciar, estadoCarga, estadoVacio, estadoError } from "../ui/mensajes.js";

const MIN_PARADAS = 2;
const MAX_PARADAS = 7; // igual que TrayectoServiceImpl: inicial + hasta 5 intermedias + final

const nombreCompleto = (p) => `${p?.nombres ?? ""} ${p?.apellidos ?? ""}`.trim() || "—";
const enlaceMapa = (codigo, texto = "Ver en el mapa") =>
    el("a", { class: "boton boton--contorno boton--chico", href: `#/mapa/${encodeURIComponent(codigo)}`,
        "aria-label": `Ver la ruta ${codigo} en el mapa` }, texto);

export function montar(contenedor, { navegar }) {
    let activa = true;
    let vehiculos = [];
    let consecutivoConductores = 0; // ignora respuestas viejas si el usuario cambia de vehículo rápido

    // ============================ 1. NUEVA RUTA =============================
    const campoVehiculo = crearCampo({ nombre: "idVehiculo", etiqueta: "Vehículo", tipo: "select", requerido: true, opciones: [] });
    const campoConductor = crearCampo({
        nombre: "idPersona", etiqueta: "Conductor", tipo: "select", requerido: true, opciones: [],
        ayuda: "Solo aparecen los conductores en estado PO (Puede operar) para el vehículo elegido.",
    });
    campoConductor.entrada.disabled = true;
    const campoCodigo = crearCampo({
        nombre: "codigoRuta", etiqueta: "Código de ruta", requerido: true, placeholder: "Ej. RUTA-IBAGUE-01",
        ayuda: "Debe ser único. Se guarda en mayúsculas.",
        atributos: { maxlength: "50", autocomplete: "off", spellcheck: "false", autocapitalize: "characters",
            pattern: "[A-Za-z0-9_\\-]+", title: "Usa solo letras, números, guiones y guion bajo, sin espacios." },
    });
    const avisoVehiculo = el("p", { class: "mensaje", role: "status" });

    const listaParadas = el("ol", { class: "paradas-editor", "aria-label": "Paradas de la ruta" });
    const botonAgregar = boton("Agregar parada", { chico: true });
    const contadorParadas = el("p", { class: "texto-tenue", role: "status" });

    const formulario = el("form", { class: "formulario", "aria-labelledby": "titulo-nueva-ruta" },
        el("div", { class: "formulario-fila" }, campoVehiculo.contenedor, campoConductor.contenedor),
        avisoVehiculo,
        campoCodigo.contenedor,
        el("fieldset", { class: "paradas-grupo" },
            el("legend", {}, "Paradas"),
            el("p", { class: "texto-tenue" }, "La primera es la parada inicial y la última, la final. ",
                "Las coordenadas son opcionales: si las dejas vacías, Google Maps las completa."),
            listaParadas,
            el("div", { class: "paradas-pie" }, botonAgregar, contadorParadas)),
        el("div", { class: "formulario-acciones" }, boton("Crear ruta", { variante: "primario", tipo: "submit" })));

    // ---------------------------- Vehículo y conductor -----------------------
    // OJO: replaceChildren NO aplana arreglos (el() sí): sin el "..." la lista se convertiría en texto
    function opcionesSelect(select, opciones, textoVacio = "Selecciona…") {
        select.replaceChildren(el("option", { value: "" }, textoVacio),
            ...opciones.map((o) => el("option", { value: o.valor }, o.texto)));
    }

    const habilitado = (v) => (v.documentos ?? []).length > 0 && v.documentos.every((d) => d.estadoDocumento === "Habilitado");

    async function cargarVehiculos() {
        try {
            const lista = await apiGet("/api/vehiculos");
            if (!activa) return;
            vehiculos = [...(lista ?? [])].sort((a, b) => a.placa.localeCompare(b.placa));
            opcionesSelect(campoVehiculo.entrada, vehiculos.map((v) => ({
                valor: String(v.id),
                texto: `${v.placa} · ${[v.marca, v.linea].filter(Boolean).join(" ")}${habilitado(v) ? "" : " (documentos sin habilitar)"}`,
            })), vehiculos.length ? "Selecciona…" : "No hay vehículos registrados");
        } catch (error) {
            if (!activa || error instanceof SesionExpiradaError) return;
            mostrarErrorCampo(campoVehiculo.entrada, `No se pudieron cargar los vehículos: ${error.message}`);
        }
    }

    campoVehiculo.entrada.addEventListener("change", async () => {
        const id = campoVehiculo.entrada.value;
        const mio = ++consecutivoConductores;
        const vehiculo = vehiculos.find((v) => String(v.id) === id);
        avisoVehiculo.textContent = "";
        avisoVehiculo.classList.remove("mensaje--error");
        campoConductor.entrada.disabled = true;
        opcionesSelect(campoConductor.entrada, [], id ? "Cargando conductores…" : "Primero elige un vehículo");
        if (!id) return;

        if (vehiculo && !habilitado(vehiculo)) {
            avisoVehiculo.textContent = "Este vehículo tiene documentos que no están en estado Habilitado: el sistema no permitirá crear la ruta hasta habilitarlos.";
            avisoVehiculo.classList.add("mensaje--error");
        }
        try {
            const conductores = await apiGet(`/api/vehiculos/${id}/conductores`);
            if (!activa || mio !== consecutivoConductores) return;
            const enPO = (conductores ?? []).filter((c) => c.estadoConductor === "PO");
            opcionesSelect(campoConductor.entrada,
                enPO.map((c) => ({ valor: String(c.idPersona), texto: `${nombreCompleto(c)} · ${c.identificacion}` })),
                enPO.length ? "Selecciona…" : "Ningún conductor en PO para este vehículo");
            campoConductor.entrada.disabled = enPO.length === 0;
            if (enPO.length === 1) campoConductor.entrada.value = String(enPO[0].idPersona); // si solo hay uno, se elige solo
            anunciar(enPO.length ? `${enPO.length} conductores disponibles para este vehículo.` : "Este vehículo no tiene conductores en estado Puede operar.");
        } catch (error) {
            if (!activa || mio !== consecutivoConductores || error instanceof SesionExpiradaError) return;
            opcionesSelect(campoConductor.entrada, [], "No se pudieron cargar");
            mostrarErrorCampo(campoConductor.entrada, error.message);
        }
    });

    campoCodigo.entrada.addEventListener("blur", () => { campoCodigo.entrada.value = campoCodigo.entrada.value.trim().toUpperCase(); });

    // -------------------------------- Paradas --------------------------------
    // Cada parada es un <li> con sus propios controles. Mover o quitar se hace moviendo el <li> en el DOM:
    // así los valores escritos y el foco se conservan sin tener que volver a dibujar todo.
    let contadorIds = 0;

    function crearParada(valores = {}) {
        const n = ++contadorIds;
        const ubicacion = crearCampo({ nombre: `ubicacion-${n}`, etiqueta: "Ubicación", requerido: true, valor: valores.ubicacion ?? "",
            placeholder: "Ej. Plaza de Bolívar, Ibagué, Tolima", atributos: { autocomplete: "off", maxlength: "255" } }); // 255 = ancho de la columna ubicacion en trayecto
        const latitud = crearCampo({ nombre: `latitud-${n}`, etiqueta: "Latitud", tipo: "number", valor: valores.latitud ?? "",
            atributos: { step: "any", min: "-90", max: "90", inputmode: "decimal" } });
        const longitud = crearCampo({ nombre: `longitud-${n}`, etiqueta: "Longitud", tipo: "number", valor: valores.longitud ?? "",
            atributos: { step: "any", min: "-180", max: "180", inputmode: "decimal" } });

        const titulo = el("h3", { class: "parada-titulo" });
        const subir = boton("Subir", { chico: true });
        const bajar = boton("Bajar", { chico: true });
        const quitar = boton("Quitar", { chico: true, variante: "peligro" });

        const li = el("li", { class: "parada-editor" },
            el("div", { class: "parada-editor-cabecera" }, titulo, el("div", { class: "acciones" }, subir, bajar, quitar)),
            ubicacion.contenedor,
            el("div", { class: "formulario-fila" }, latitud.contenedor, longitud.contenedor));
        li._campos = { ubicacion: ubicacion.entrada, latitud: latitud.entrada, longitud: longitud.entrada, titulo, subir, bajar, quitar };

        subir.addEventListener("click", () => mover(li, -1, subir));
        bajar.addEventListener("click", () => mover(li, +1, bajar));
        quitar.addEventListener("click", () => quitarParada(li));
        return li;
    }

    const paradas = () => [...listaParadas.children];

    // Recalcula títulos (Parada 0 · inicial...), etiquetas accesibles y qué botones se pueden usar
    function renumerar() {
        const lista = paradas();
        lista.forEach((li, i) => {
            const c = li._campos;
            const rol = i === 0 ? "inicial" : i === lista.length - 1 ? "final" : "intermedia";
            c.titulo.replaceChildren(`Parada ${i + 1} `, etiqueta(rol, i === 0 ? "ok" : i === lista.length - 1 ? "lima" : ""),
                el("span", { class: "texto-tenue" }, ` · orden ${i}`));
            c.subir.setAttribute("aria-label", `Subir la parada ${i + 1}`);
            c.bajar.setAttribute("aria-label", `Bajar la parada ${i + 1}`);
            c.quitar.setAttribute("aria-label", `Quitar la parada ${i + 1}`);
            // aria-disabled (no disabled): el botón sigue en el orden de tabulación y el foco no se pierde al llegar al borde
            c.subir.setAttribute("aria-disabled", String(i === 0));
            c.bajar.setAttribute("aria-disabled", String(i === lista.length - 1));
            c.quitar.setAttribute("aria-disabled", String(lista.length <= MIN_PARADAS));
        });
        botonAgregar.setAttribute("aria-disabled", String(lista.length >= MAX_PARADAS));
        contadorParadas.textContent = `${lista.length} de ${MAX_PARADAS} paradas (mínimo ${MIN_PARADAS}).`;
    }

    function agregarParada() {
        if (paradas().length >= MAX_PARADAS) { anunciar(`Máximo ${MAX_PARADAS} paradas.`); return; }
        const li = crearParada();
        listaParadas.append(li);
        renumerar();
        li._campos.ubicacion.focus();
        anunciar(`Parada ${paradas().length} agregada.`);
    }

    function quitarParada(li) {
        if (paradas().length <= MIN_PARADAS) { anunciar(`Una ruta necesita al menos ${MIN_PARADAS} paradas.`); return; }
        const siguiente = li.nextElementSibling ?? li.previousElementSibling;
        const numero = paradas().indexOf(li) + 1;
        li.remove();
        renumerar();
        (siguiente?._campos.ubicacion ?? botonAgregar).focus(); // el foco nunca se queda "en el aire"
        anunciar(`Parada ${numero} quitada.`);
    }

    function mover(li, delta, botonPulsado) {
        const lista = paradas();
        const i = lista.indexOf(li);
        const destino = i + delta;
        if (destino < 0 || destino >= lista.length) return;
        if (delta < 0) listaParadas.insertBefore(li, lista[destino]);
        else listaParadas.insertBefore(li, lista[destino].nextElementSibling);
        renumerar();
        botonPulsado.focus(); // el foco acompaña a la parada movida
        anunciar(`Parada movida a la posición ${destino + 1}.`);
    }

    botonAgregar.addEventListener("click", agregarParada);

    function reiniciarParadas() {
        listaParadas.replaceChildren(crearParada(), crearParada());
        renumerar();
    }
    reiniciarParadas();

    // -------------------------------- Envío ----------------------------------
    // Lee una coordenada opcional. Devuelve { valor } o { error } (vacío = null, que es válido)
    function leerCoordenada(entrada, min, max, nombre) {
        const texto = entrada.value.trim();
        if (!texto) return { valor: null };
        const numero = Number(texto);
        if (!Number.isFinite(numero) || numero < min || numero > max) return { error: `${nombre} debe estar entre ${min} y ${max}.` };
        return { valor: numero };
    }

    enviarFormulario(formulario, {
        textoCargando: "Creando ruta…",
        alEnviar: async (form) => {
            const codigoRuta = campoCodigo.entrada.value.trim().toUpperCase();
            campoCodigo.entrada.value = codigoRuta;
            const idPersona = Number(campoConductor.entrada.value);
            const idVehiculo = Number(campoVehiculo.entrada.value);

            let primerError = null;
            const cuerpo = paradas().map((li, ordenParada) => {
                const c = li._campos;
                const lat = leerCoordenada(c.latitud, -90, 90, "La latitud");
                const lng = leerCoordenada(c.longitud, -180, 180, "La longitud");
                if (lat.error) { mostrarErrorCampo(c.latitud, lat.error); primerError ??= c.latitud; }
                if (lng.error) { mostrarErrorCampo(c.longitud, lng.error); primerError ??= c.longitud; }
                // Las dos o ninguna: media coordenada no sirve para ubicar la parada en el mapa
                if (!lat.error && !lng.error && (lat.valor === null) !== (lng.valor === null)) {
                    const falta = lat.valor === null ? c.latitud : c.longitud;
                    mostrarErrorCampo(falta, "Escribe la latitud y la longitud, o deja las dos vacías.");
                    primerError ??= falta;
                }
                return { idPersona, idVehiculo, codigoRuta, ubicacion: c.ubicacion.value.trim(), ordenParada,
                    latitud: lat.valor, longitud: lng.valor };
            });
            if (primerError) { mostrarErrorGeneral(form, "Revisa las coordenadas marcadas."); primerError.focus(); return; }

            await apiPost("/api/trayectos/rutas", cuerpo); // 201 sin cuerpo. Los errores (400/404) los muestra enviarFormulario
            avisar(`Ruta ${codigoRuta} creada con ${cuerpo.length} paradas.`, "ok");
            navegar(`/mapa/${encodeURIComponent(codigoRuta)}`);
        },
    });

    // ============================== 2. CONSULTAS ==============================
    function crearConsulta({ id, titulo, descripcion, campo, ruta, pintar, vacio }) {
        const entrada = crearCampo(campo);
        const resultado = el("div", { class: "consulta-ruta-resultado", "aria-live": "polite" });
        const form = el("form", { class: "formulario", "aria-labelledby": `titulo-${id}` },
            entrada.contenedor,
            el("div", { class: "formulario-acciones" }, boton("Consultar", { variante: "primario", tipo: "submit" })));
        enviarFormulario(form, {
            textoCargando: "Consultando…",
            alEnviar: async () => {
                const valor = entrada.entrada.value.trim().toUpperCase();
                resultado.replaceChildren(estadoCarga("Consultando…"));
                try {
                    const datos = await apiGet(ruta(valor));
                    if (!activa) return;
                    if (!datos?.length) { resultado.replaceChildren(estadoVacio(vacio(valor))); anunciar(vacio(valor)); return; }
                    resultado.replaceChildren(pintar(datos));
                    anunciar(`${datos.length} ${datos.length === 1 ? "ruta encontrada" : "rutas encontradas"}.`);
                } catch (error) {
                    if (!activa || error instanceof SesionExpiradaError) return;
                    // 404 (no existe la persona o el vehículo) y 400 (no es conductor) traen un mensaje claro del servidor
                    resultado.replaceChildren([400, 404].includes(error.estado)
                        ? estadoVacio(error.message)
                        : estadoError(error.message));
                }
            },
        });
        return el("section", { class: "tarjeta consulta-ruta", "aria-labelledby": `titulo-${id}` },
            el("h3", { id: `titulo-${id}` }, titulo),
            el("p", { class: "texto-tenue" }, descripcion),
            form, resultado);
    }

    const consultaConductor = crearConsulta({
        id: "rutas-conductor",
        titulo: "Por conductor",
        descripcion: "Los códigos de las rutas asignadas a un conductor.",
        campo: { nombre: "identificacion", etiqueta: "Identificación del conductor", requerido: true,
            atributos: { inputmode: "numeric", autocomplete: "off", pattern: "\\d+", title: "Escribe solo números, sin puntos ni espacios." } },
        ruta: (v) => `/api/trayectos/conductores/${encodeURIComponent(v)}/rutas`,
        vacio: (v) => `El conductor ${v} no tiene rutas asignadas`,
        pintar: (codigos) => el("ul", { class: "rutas-codigos" },
            codigos.map((c) => el("li", {}, enlaceMapa(c, c)))),
    });

    const consultaPlaca = crearConsulta({
        id: "rutas-placa",
        titulo: "Por placa",
        descripcion: "Las rutas de un vehículo, con su conductor y número de paradas.",
        campo: { nombre: "placa", etiqueta: "Placa", requerido: true, placeholder: "Ej. ABC123",
            atributos: { maxlength: "6", autocomplete: "off", autocapitalize: "characters" } },
        ruta: (v) => `/api/trayectos/vehiculos/${encodeURIComponent(v)}/rutas`,
        vacio: (v) => `El vehículo ${v} no tiene rutas registradas`,
        pintar: (rutas) => el("ul", { class: "rutas-tarjetas" }, rutas.map((r) =>
            el("li", { class: "tarjeta ruta-tarjeta" },
                el("strong", { class: "mono" }, r.codigoRuta),
                el("span", {}, nombreCompleto(r.conductor),
                    r.conductor?.identificacion ? el("span", { class: "texto-tenue" }, ` · ${r.conductor.identificacion}`) : null),
                el("span", { class: "texto-tenue" }, `${r.totalParadas} ${r.totalParadas === 1 ? "parada" : "paradas"}`),
                enlaceMapa(r.codigoRuta)))),
    });

    contenedor.append(
        encabezadoVista({
            sobretitulo: "Entrega 3", antes: "Rutas y ", destacado: "trayectos",
            descripcion: "Crea una ruta con sus paradas en orden, o consulta las rutas de un conductor o de un vehículo.",
        }),
        el("div", { class: "rutas-layout" },
            el("section", { class: "tarjeta", "aria-labelledby": "titulo-nueva-ruta" },
                el("h2", { id: "titulo-nueva-ruta", class: "titulo-seccion" }, "Nueva ruta"),
                formulario),
            el("section", { class: "rutas-consultas", "aria-labelledby": "titulo-consultas-rutas" },
                el("h2", { id: "titulo-consultas-rutas", class: "titulo-seccion" }, "Consultar rutas"),
                consultaConductor, consultaPlaca)));

    opcionesSelect(campoConductor.entrada, [], "Primero elige un vehículo");
    opcionesSelect(campoVehiculo.entrada, [], "Cargando vehículos…");
    cargarVehiculos();
    return () => { activa = false; };
}

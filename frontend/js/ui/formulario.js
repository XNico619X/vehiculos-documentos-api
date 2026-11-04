// ============================================================================
// ui/formulario.js · CAMPOS, VALIDACIÓN Y ENVÍO DE FORMULARIOS
// ============================================================================
// Hace tres cosas que TODOS los formularios de la app necesitan:
//
//  1. crearCampo()        arma un campo completo: etiqueta + control + ayuda + error,
//                         ya enlazados para lectores de pantalla (for / aria-describedby / aria-invalid).
//  2. enviarFormulario()  conecta el submit: valida, deshabilita el botón mientras envía,
//                         evita el doble envío y muestra los errores.
//  3. errores del SERVIDOR junto al campo: el backend responde 400 con detalles como
//                         "placa: size must be between 6 and 6". Se usa el nombre antes de los ":" para
//                         encontrar el campo con ese `name` y escribir el mensaje debajo de él.
//                         Lo que no corresponda a ningún campo va a un aviso general arriba del formulario.
//
// CONVENCIÓN IMPORTANTE: el atributo `name` de cada campo = el nombre del campo del DTO de Java
// (placa, tipoVehiculo, numeroIdentificacion...). Así los errores del servidor caen donde deben.

import { SesionExpiradaError } from "../api.js";
import { el } from "./util.js";

let contador = 0;

// ------------------------------- 1. CAMPOS -----------------------------------
// crearCampo({
//   nombre: "placa", etiqueta: "Placa", tipo: "text" | "password" | "number" | "date" | "file" | "select" | "textarea" | "checkbox",
//   requerido: true, ayuda: "Formato ABC123", valor: "", opciones: [{ valor: "A", texto: "Automóvil" }] (solo select),
//   placeholder, atributos: { maxlength: 6, autocomplete: "username" }
// })
// Devuelve { contenedor, entrada } → contenedor se agrega al formulario; entrada es el <input> (para leer o escribir su valor).
export function crearCampo({
    nombre, etiqueta, tipo = "text", requerido = false, ayuda = "", valor = "",
    opciones = [], placeholder = "", atributos = {},
}) {
    const id = `campo-${nombre}-${++contador}`; // id único: dos formularios pueden tener un campo "placa"
    const idAyuda = `${id}-ayuda`;
    const idError = `${id}-error`;

    let entrada;
    if (tipo === "select") {
        entrada = el("select", { id, name: nombre },
            // Opción vacía primero: obliga a elegir de forma consciente (en vez de enviar la primera por accidente)
            el("option", { value: "" }, "Selecciona…"),
            opciones.map((o) => el("option", { value: o.valor }, o.texto)));
        entrada.value = valor;
    } else if (tipo === "textarea") {
        entrada = el("textarea", { id, name: nombre, placeholder });
        entrada.value = valor;
    } else if (tipo === "checkbox") {
        entrada = el("input", { id, name: nombre, type: "checkbox" });
        entrada.checked = Boolean(valor);
    } else {
        entrada = el("input", { id, name: nombre, type: tipo, placeholder });
        if (tipo !== "file") entrada.value = valor;
    }
    for (const [k, v] of Object.entries(atributos)) entrada.setAttribute(k, v);
    if (requerido) entrada.required = true;

    // aria-describedby: el lector de pantalla lee la ayuda y el error después del nombre del campo
    entrada.setAttribute("aria-describedby", ayuda ? `${idAyuda} ${idError}` : idError);

    const etiquetaEl = el("label", { for: id }, etiqueta,
        // El asterisco es solo visual: "aria-hidden" evita que el lector diga "asterisco"; ya dice "obligatorio" por `required`
        requerido ? el("span", { class: "req", "aria-hidden": "true" }, " *") : null);

    const error = el("p", { class: "campo-error", id: idError, hidden: true });

    const contenedor = tipo === "checkbox"
        ? el("div", { class: "campo campo--casilla" }, entrada, etiquetaEl, error)
        : el("div", { class: "campo" }, etiquetaEl, entrada,
            ayuda ? el("p", { class: "campo-ayuda", id: idAyuda }, ayuda) : null, error);

    // En cuanto el usuario corrige el campo, su error desaparece
    entrada.addEventListener("input", () => limpiarErrorCampo(entrada));
    entrada.addEventListener("change", () => limpiarErrorCampo(entrada));

    return { contenedor, entrada };
}

// Botón "Mostrar / Ocultar" para un campo de contraseña o clave.
//   const campo = crearCampo({ nombre: "password", tipo: "password", ... });
//   agregarMostrarOcultar(campo.entrada);
// aria-pressed dice al lector de pantalla si el texto está visible; aria-controls, qué campo cambia.
// El botón NO lleva el nombre del campo en su texto visible, pero sí en el aria-label ("Mostrar Contraseña").
export function agregarMostrarOcultar(entrada) {
    const nombre = entrada.labels?.[0]?.firstChild?.textContent?.trim()
        || entrada.closest(".campo")?.querySelector("label")?.firstChild?.textContent?.trim() || "valor";
    const alternar = el("button", {
        type: "button", class: "campo-alternar", "aria-pressed": "false", "aria-controls": entrada.id,
        "aria-label": `Mostrar ${nombre}`,
    }, "Mostrar");
    alternar.addEventListener("click", () => {
        const visible = entrada.type === "password";
        entrada.type = visible ? "text" : "password";
        alternar.textContent = visible ? "Ocultar" : "Mostrar";
        alternar.setAttribute("aria-pressed", String(visible));
        alternar.setAttribute("aria-label", `${visible ? "Ocultar" : "Mostrar"} ${nombre}`);
    });
    // El input y el botón quedan en una misma fila; el resto del campo (ayuda, error) no cambia
    const fila = el("div", { class: "campo-con-boton" });
    entrada.replaceWith(fila);
    fila.append(entrada, alternar);
    return alternar;
}

// Vuelve a ocultar un campo que el usuario dejó visible (por ejemplo, al limpiar el formulario)
export function ocultarValor(entrada) {
    const alternar = entrada.parentElement?.querySelector(".campo-alternar");
    if (alternar && entrada.type === "text") alternar.click();
}

// Escribe un error debajo de un campo y lo marca como inválido (borde rojo + aria-invalid).
// Funciona con cualquier control que esté dentro de un <div class="campo"> con un <p class="campo-error">.
export function mostrarErrorCampo(entrada, mensaje) {
    const error = entrada.closest(".campo")?.querySelector(".campo-error");
    if (!error) return;
    error.textContent = mensaje;
    error.hidden = false;
    entrada.setAttribute("aria-invalid", "true");
}

export function limpiarErrorCampo(entrada) {
    const error = entrada.closest(".campo")?.querySelector(".campo-error");
    if (error) { error.textContent = ""; error.hidden = true; }
    entrada.removeAttribute("aria-invalid");
}

// ------------------------------ 2. ENVÍO ------------------------------------
// enviarFormulario(form, {
//     alEnviar: async (form) => { ... llamar a la API ... },   // si lanza un error, se muestra
//     textoCargando: "Guardando…",                             // texto del botón mientras envía
// })
// Dentro de alEnviar se lee el formulario con datosFormulario(form).
export function enviarFormulario(form, { alEnviar, textoCargando = "Enviando…" }) {
    // Desactiva la validación NATIVA del navegador (esas burbujas de "Rellena este campo" no son
    // accesibles ni controlables). Se valida abajo con nuestros propios mensajes, junto a cada campo.
    form.noValidate = true;

    form.addEventListener("submit", async (evento) => {
        evento.preventDefault(); // evita que el navegador recargue la página
        const boton = form.querySelector('[type="submit"]');
        if (form.dataset.enviando === "1") return; // ya hay un envío en curso: se ignora el doble clic

        limpiarErroresFormulario(form);
        if (!validarCampos(form)) return;

        form.dataset.enviando = "1";
        const textoOriginal = boton?.textContent;
        if (boton) {
            boton.setAttribute("aria-busy", "true");
            boton.setAttribute("aria-disabled", "true"); // aria-disabled y no "disabled": así no pierde el foco
            boton.textContent = textoCargando;
        }
        try {
            await alEnviar(form);
        } catch (error) {
            if (!(error instanceof SesionExpiradaError)) mostrarErroresServidor(form, error); // la sesión la maneja app.js
        } finally {
            delete form.dataset.enviando;
            if (boton && boton.isConnected) { // la vista pudo cambiar mientras tanto
                boton.removeAttribute("aria-busy");
                boton.removeAttribute("aria-disabled");
                boton.textContent = textoOriginal;
            }
        }
    });
}

// Revisa las reglas HTML de cada control (required, minlength, pattern, type=email...) con
// la API nativa checkValidity(), pero muestra el mensaje con NUESTRO diseño. Enfoca el primer error.
function validarCampos(form) {
    let primero = null;
    for (const control of form.elements) {
        if (!control.willValidate || control.validity.valid) continue;
        mostrarErrorCampo(control, mensajeValidacion(control));
        primero ??= control;
    }
    if (primero) { primero.focus(); return false; }
    return true;
}

// Mensajes en español. control.validationMessage vendría en el idioma del NAVEGADOR (a veces en
// inglés), así que se traducen aquí según la regla que falló.
function mensajeValidacion(control) {
    const v = control.validity;
    if (v.valueMissing) return control.type === "file" ? "Selecciona un archivo." : "Este campo es obligatorio.";
    if (v.typeMismatch) return "El formato no es válido.";
    if (v.patternMismatch) return control.title || "El formato no es válido.";
    if (v.tooShort) return `Escribe al menos ${control.minLength} caracteres.`;
    if (v.tooLong) return `Máximo ${control.maxLength} caracteres.`;
    if (v.rangeUnderflow) return `El valor mínimo es ${control.min}.`;
    if (v.rangeOverflow) return `El valor máximo es ${control.max}.`;
    if (v.stepMismatch || v.badInput) return "Escribe un valor válido.";
    return control.validationMessage;
}

// ----------------- 3. ERRORES DEL SERVIDOR JUNTO AL CAMPO --------------------
export function mostrarErroresServidor(form, error) {
    const sueltos = [];
    let primero = null;

    for (const detalle of error.detalles ?? []) {
        const posicion = String(detalle).indexOf(": "); // "placa: mensaje" -> campo "placa"
        const nombre = posicion > 0 ? detalle.slice(0, posicion) : "";
        const control = nombre ? form.elements.namedItem(nombre) : null;
        if (control instanceof Element) { // namedItem puede devolver null o un grupo de radios
            mostrarErrorCampo(control, detalle.slice(posicion + 2));
            primero ??= control;
        } else {
            sueltos.push(detalle);
        }
    }

    if (primero) {
        mostrarErrorGeneral(form, "Revisa los campos marcados.", sueltos);
        primero.focus();
    } else {
        // Un error que no es de un campo (ej. "La placa ya existe", "Mínimo un documento"): se muestra tal cual
        mostrarErrorGeneral(form, error.message || "No se pudo completar la acción.", sueltos);
    }
}

// Zona de error general: un <div role="alert"> al inicio del formulario (se crea la primera vez)
export function mostrarErrorGeneral(form, texto, lista = []) {
    let zona = form.querySelector(".formulario-error");
    if (!zona) {
        zona = el("div", { class: "formulario-error", role: "alert" });
        form.prepend(zona);
    }
    // replaceChildren convierte null en el texto "null": la lista solo se agrega si hay elementos
    zona.replaceChildren(texto);
    if (lista.length) zona.append(el("ul", {}, lista.map((t) => el("li", {}, t))));
    zona.hidden = false;
}

export function limpiarErroresFormulario(form) {
    form.querySelector(".formulario-error")?.setAttribute("hidden", "");
    for (const control of form.elements) {
        if (control.closest?.(".campo")) limpiarErrorCampo(control);
    }
}

// ------------------------------ Lectura de datos -----------------------------
// Convierte el formulario en un objeto { nombreDelCampo: valor } listo para enviar como JSON:
//   · textos sin espacios sobrantes (trim)   · casillas -> true/false   · campos vacíos -> "" (decide quien lo usa)
// Los campos de archivo se omiten (se leen aparte con entrada.files[0]).
export function datosFormulario(form) {
    const datos = {};
    for (const control of form.elements) {
        if (!control.name || control.type === "file" || control.type === "submit" || control.type === "button") continue;
        datos[control.name] = control.type === "checkbox" ? control.checked
            : typeof control.value === "string" ? control.value.trim() : control.value;
    }
    return datos;
}

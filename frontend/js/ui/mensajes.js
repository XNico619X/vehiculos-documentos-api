// ============================================================================
// ui/mensajes.js · AVISOS, ANUNCIOS Y BLOQUES DE ESTADO
// ============================================================================
//   avisar(texto, tipo)      aviso flotante (toast) abajo a la derecha
//   mostrarError(error)      avisa un error de la API (ignora el de "sesión expirada": app.js ya lo maneja)
//   anunciar(texto)          texto invisible que anuncia el lector de pantalla
//   escribirMensaje(el, ...) mensaje en línea (un <p class="mensaje">)
//   estadoCarga / estadoVacio / estadoError   bloques para "cargando", "sin datos" y "falló"

import { SesionExpiradaError } from "../api.js";
import { el, boton } from "./util.js";

const MAX_AVISOS = 4;

// ----------------------------- Avisos (toasts) -------------------------------
// El contenedor #avisos (index.html) ya existe con aria-live="polite" ANTES de que se
// escriba nada en él; por eso los lectores de pantalla anuncian cada aviso nuevo.
//   tipo: "info" | "ok" | "aviso" | "error"
// Los de error NO se cierran solos (el usuario necesita tiempo para leerlos): quedan hasta pulsar ×.
export function avisar(texto, tipo = "info") {
    const contenedor = document.getElementById("avisos");
    if (!contenedor) return;

    // Un error es urgente: role="alert" lo anuncia de inmediato. El resto se anuncia con calma ("status").
    const aviso = el("div", { class: `aviso aviso--${tipo}`, role: tipo === "error" ? "alert" : "status" },
        el("p", { class: "aviso-texto" }, texto),
        el("button", {
            class: "aviso-cerrar", type: "button", "aria-label": "Cerrar aviso",
            onclick: () => aviso.remove(),
        }, "×"));
    contenedor.append(aviso);

    while (contenedor.children.length > MAX_AVISOS) contenedor.firstElementChild.remove(); // no acumular

    if (tipo !== "error") setTimeout(() => aviso.remove(), 6000);
    return aviso;
}

// Atajo para el catch de cualquier acción: avisa el mensaje del error, salvo que sea "sesión expirada"
// (en ese caso api.js ya cerró la sesión y app.js muestra el motivo y lleva al login).
export function mostrarError(error) {
    if (error instanceof SesionExpiradaError) return;
    avisar(error?.message || "Ocurrió un error inesperado.", "error");
}

// ------------------------- Anuncio para lectores de pantalla ------------------
// Se vacía y se vuelve a llenar en el siguiente "tick": si el texto fuera idéntico al anterior
// (ej. dos veces "Cargando"), el lector de pantalla no lo repetiría.
export function anunciar(texto) {
    const zona = document.getElementById("anuncio");
    if (!zona) return;
    zona.textContent = "";
    setTimeout(() => { zona.textContent = texto; }, 50);
}

// --------------------------- Mensaje en línea --------------------------------
// Para un <p class="mensaje" role="status"> de una vista. tipo: "" | "error" | "ok"
export function escribirMensaje(elemento, texto, tipo = "") {
    elemento.textContent = texto;
    elemento.classList.toggle("mensaje--error", tipo === "error");
    elemento.classList.toggle("mensaje--ok", tipo === "ok");
}

// ------------------------ Estados: cargando / vacío / error -------------------
// Toda vista que pide datos pone uno de estos bloques mientras tanto:
//   contenedor.replaceChildren(estadoCarga());
//   ... y luego lo reemplaza por la tabla, o por estadoVacio(...) / estadoError(...)

export function estadoCarga(texto = "Cargando…") {
    return el("div", { class: "estado", role: "status" },
        el("div", { class: "cargador", "aria-hidden": "true" }),
        el("p", {}, texto));
}

// accion (opcional): { texto: "Crear vehículo", alClic: fn }
export function estadoVacio(titulo, texto = "", accion = null) {
    return el("div", { class: "estado" },
        el("h3", {}, titulo),
        texto ? el("p", {}, texto) : null,
        accion ? boton(accion.texto, { variante: "primario", chico: true, onclick: accion.alClic }) : null);
}

// alReintentar (opcional): función que vuelve a pedir los datos
export function estadoError(mensaje, alReintentar = null) {
    return el("div", { class: "estado estado--error", role: "alert" },
        el("h3", {}, "No se pudo cargar"),
        el("p", {}, mensaje),
        alReintentar ? boton("Reintentar", { variante: "contorno", chico: true, onclick: alReintentar }) : null);
}

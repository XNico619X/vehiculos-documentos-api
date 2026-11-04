// ============================================================================
// ui/credenciales.js · MOSTRAR CREDENCIALES UNA SOLA VEZ
// ============================================================================
// El backend devuelve la contraseña y la APIKey SOLO en la respuesta que las crea
// (primer administrador, crear un administrativo, regenerar la APIKey). Después no hay
// forma de volver a verlas, así que este diálogo:
//   · no se cierra con Esc ni con clic fuera (cancelable: false)
//   · tiene un botón "Copiar" por cada valor y uno para copiar todo
//   · solo deja continuar cuando el usuario marca "Ya las guardé"
//
//   await mostrarCredenciales({ titulo: "Primer administrador creado", login, password, apikey });
//   (password o apikey pueden omitirse: regenerar la APIKey solo muestra la clave nueva)

import { mostrarDialogo } from "./dialogo.js";
import { el, boton } from "./util.js";
import { anunciar } from "./mensajes.js";

// navigator.clipboard solo existe en contextos seguros (https o localhost). Si no está, o el
// navegador lo niega, se selecciona el texto para que el usuario pulse Ctrl+C.
async function copiar(texto, nodoValor) {
    try {
        await navigator.clipboard.writeText(texto);
        anunciar("Copiado al portapapeles.");
        return true;
    } catch {
        const rango = document.createRange();
        rango.selectNodeContents(nodoValor);
        const seleccion = window.getSelection();
        seleccion.removeAllRanges();
        seleccion.addRange(rango);
        anunciar("No se pudo copiar solo. El texto quedó seleccionado: pulsa Ctrl+C.");
        return false;
    }
}

function filaCredencial(nombre, valor) {
    const nodoValor = el("code", { class: "credencial-valor" }, valor);
    const botonCopiar = boton("Copiar", { chico: true, "aria-label": `Copiar ${nombre}` });
    botonCopiar.addEventListener("click", async () => {
        const ok = await copiar(valor, nodoValor);
        if (!ok) return;
        botonCopiar.textContent = "Copiado ✓";
        setTimeout(() => { if (botonCopiar.isConnected) botonCopiar.textContent = "Copiar"; }, 2000);
    });
    return el("div", { class: "credencial" },
        el("dt", { class: "etiqueta-texto" }, nombre),
        el("dd", {}, nodoValor, botonCopiar));
}

export async function mostrarCredenciales({ titulo, texto, login, password, apikey }) {
    const pares = [["Usuario", login], ["Contraseña", password], ["APIKey", apikey]].filter(([, v]) => v);
    const todo = pares.map(([n, v]) => `${n}: ${v}`).join("\n");

    const casilla = el("input", { type: "checkbox", id: "credenciales-guardadas" });
    const botonCopiarTodo = boton("Copiar todo", { chico: true });
    const listaValores = el("dl", { class: "credenciales" }, pares.map(([n, v]) => filaCredencial(n, v)));
    botonCopiarTodo.addEventListener("click", () => copiar(todo, listaValores));

    const contenido = el("div", { class: "credenciales-cuerpo" },
        el("p", { class: "formulario-error", role: "alert" },
            texto ?? "Guarda estos datos ahora: es la única vez que se muestran y no se pueden recuperar."),
        listaValores,
        pares.length > 1 ? botonCopiarTodo : null,
        el("div", { class: "campo campo--casilla" },
            casilla, el("label", { for: "credenciales-guardadas" }, "Ya las guardé en un lugar seguro")));

    await mostrarDialogo({
        titulo,
        contenido,
        cancelable: false,
        acciones: [{ texto: "Continuar", valor: "continuar", variante: "primario" }],
        // "Continuar" queda deshabilitado hasta marcar la casilla. Se usa aria-disabled + bloqueo del clic
        // (y no el atributo disabled) para que el botón siga siendo alcanzable y el lector diga por qué no responde.
        alMontar: (dialogo, [continuar]) => {
            const actualizar = () => continuar.setAttribute("aria-disabled", String(!casilla.checked));
            actualizar();
            casilla.addEventListener("change", actualizar);
            continuar.setAttribute("aria-describedby", "credenciales-guardadas");
            continuar.addEventListener("click", (e) => {
                if (!casilla.checked) { e.preventDefault(); casilla.focus(); }
            });
            dialogo.querySelector(".credencial .boton")?.focus();
        },
    });
}

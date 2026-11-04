// ============================================================================
// ui/dialogo.js · DIÁLOGOS CON EL ELEMENTO NATIVO <dialog>
// ============================================================================
// Se usa <dialog> y showModal() en lugar de inventar una ventanita con <div>
// porque el NAVEGADOR ya resuelve lo difícil de accesibilidad:
//   · atrapa el foco dentro del diálogo (Tab no se sale)
//   · el resto de la página queda inerte mientras está abierto
//   · Esc lo cierra
//   · al cerrar, devuelve el foco al botón que lo abrió
//
//   const ok = await confirmar({ titulo: "Eliminar vehículo", texto: "…", peligro: true });
//   if (ok) { ... }

import { el, boton } from "./util.js";

let contador = 0; // para generar ids únicos (aria-labelledby / aria-describedby)

// Primitiva general. Devuelve una promesa con el `valor` del botón pulsado (o null si se cerró con Esc / clic fuera).
//   titulo, contenido (nodo o texto), acciones: [{ texto, valor, variante, enfocar }]
//   cancelable: false -> Esc y clic fuera NO cierran (para avisos que el usuario debe leer, ej. credenciales una sola vez)
//   alMontar(dialogo, botones): se llama al abrir; sirve para, por ejemplo, deshabilitar un botón hasta marcar una casilla
export function mostrarDialogo({ titulo, contenido, acciones = [], cancelable = true, alMontar = null }) {
    return new Promise((resolver) => {
        const id = ++contador;
        const botones = acciones.map((a) =>
            boton(a.texto, { variante: a.variante ?? "contorno", value: a.valor ?? "", autofocus: a.enfocar ? "" : null }));

        // <form method="dialog">: al pulsar cualquier botón submit el navegador cierra el <dialog>
        // y deja el atributo value del botón en dialogo.returnValue. Cero JavaScript para cerrar.
        const dialogo = el("dialog", {
            class: "dialogo", "aria-labelledby": `dlg-titulo-${id}`, "aria-describedby": `dlg-texto-${id}`,
        },
            el("form", { method: "dialog", class: "dialogo-cuerpo" },
                el("h2", { id: `dlg-titulo-${id}` }, titulo),
                el("div", { class: "dialogo-texto", id: `dlg-texto-${id}` }, contenido),
                el("div", { class: "dialogo-acciones" }, botones)));

        botones.forEach((b) => { b.type = "submit"; });

        if (!cancelable) {
            // Evento "cancel" = el usuario pulsó Esc. Se anula.
            dialogo.addEventListener("cancel", (e) => e.preventDefault());
        } else {
            // Clic en el fondo oscuro (::backdrop): el destino del clic es el propio <dialog>
            dialogo.addEventListener("click", (e) => { if (e.target === dialogo) dialogo.close(); });
        }

        dialogo.addEventListener("close", () => {
            const valor = dialogo.returnValue || null;
            dialogo.remove();
            resolver(valor);
        });

        document.body.append(dialogo);
        dialogo.showModal();
        alMontar?.(dialogo, botones);
    });
}

// Pregunta de sí/no. Devuelve true solo si el usuario pulsa el botón de confirmar.
// El foco inicial queda en "Cancelar": es la opción segura si alguien pulsa Enter sin pensar.
export async function confirmar({ titulo, texto, textoConfirmar = "Confirmar", textoCancelar = "Cancelar", peligro = false }) {
    const valor = await mostrarDialogo({
        titulo,
        contenido: texto,
        acciones: [
            { texto: textoCancelar, valor: "cancelar", variante: "contorno", enfocar: true },
            { texto: textoConfirmar, valor: "confirmar", variante: peligro ? "peligro" : "primario" },
        ],
    });
    return valor === "confirmar";
}

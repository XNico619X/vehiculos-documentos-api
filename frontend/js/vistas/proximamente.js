// ============================================================================
// vistas/proximamente.js · PANTALLA TEMPORAL
// ============================================================================
// Mientras una pantalla (Partes 2 a 6) no está construida, el menú ya la muestra y
// la protege con sesión, pero abre esta vista. Al construir la pantalla real:
//   1. se crea su archivo en js/vistas/ (por ejemplo vehiculos.js)
//   2. en router.js se cambia su "cargar" de `proximamente` a su import().
// Los textos (parte, titular, descripcion) salen de la definición de la ruta en router.js.

import { el, encabezadoVista, etiqueta } from "../ui/util.js";

export function montar(contenedor, { ruta }) {
    const [antes, destacado] = ruta.titular ?? [ruta.titulo, ""];
    contenedor.append(
        encabezadoVista({ sobretitulo: `Parte ${ruta.parte}`, antes, destacado, descripcion: ruta.descripcion }),
        el("section", { class: "tarjeta proximamente" },
            etiqueta("En construcción", "lima"),
            el("p", {}, "Esta pantalla ya está en el menú y protegida por sesión. Su contenido se construye en la Parte ",
                String(ruta.parte), " del plan.")));
}

// ============================================================================
// ui/tabla.js · TABLA ARMADA A PARTIR DE DATOS
// ============================================================================
// Una sola función para todas las listas de la app (vehículos, personas, documentos...).
//
//   const tabla = crearTabla({
//       titulo: "Vehículos registrados",              // lo anuncia el lector de pantalla
//       columnas: [
//           { clave: "placa", titulo: "Placa" },                                   // muestra fila.placa
//           { clave: "estado", titulo: "Estado",
//             render: (fila) => etiqueta(fila.estado, "ok") },                     // contenido propio (nodo o texto)
//           { titulo: "Acciones", clase: "celda-acciones",
//             render: (fila) => boton("Editar", { chico: true, onclick: () => editar(fila) }) },
//       ],
//       filas: vehiculos,
//       vacio: { titulo: "No hay vehículos", texto: "Crea el primero con el botón de arriba." },
//   });
//   contenedor.append(tabla);
//
// Si `filas` está vacía NO se dibuja una tabla vacía: se muestra el mensaje de "sin datos".
// Todo texto entra con textContent (seguro contra XSS): si necesitas formato, devuelve un nodo desde render().

import { el } from "./util.js";
import { estadoVacio } from "./mensajes.js";

export function crearTabla({ titulo, columnas, filas, vacio = {} }) {
    if (!filas || filas.length === 0) {
        return estadoVacio(vacio.titulo ?? "No hay datos para mostrar", vacio.texto ?? "", vacio.accion ?? null);
    }

    const encabezado = el("tr", {}, columnas.map((c) =>
        // scope="col": el lector de pantalla sabe que esta celda titula toda la columna
        el("th", { scope: "col", class: c.alinear === "der" ? "der" : null }, c.titulo)));

    const cuerpo = filas.map((fila, indice) =>
        el("tr", {}, columnas.map((c) => {
            const contenido = c.render ? c.render(fila, indice) : fila[c.clave];
            return el("td", { class: c.clase ?? null }, contenido ?? "—"); // null/undefined -> raya, no "undefined"
        })));

    // El contenedor tiene scroll horizontal. tabindex="0" + role="region" + aria-label permiten
    // desplazarlo con el teclado (flechas) y que el lector de pantalla diga qué es.
    return el("div", { class: "tabla-contenedor", role: "region", tabindex: "0", "aria-label": titulo ?? "Tabla" },
        el("table", { class: "tabla" },
            titulo ? el("caption", { class: "solo-lectores" }, titulo) : null,
            el("thead", {}, encabezado),
            el("tbody", {}, cuerpo)));
}

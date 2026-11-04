// ============================================================================
// ui/util.js · AYUDAS PARA CONSTRUIR LA INTERFAZ
// ============================================================================
// Aquí vive el "ladrillo" básico de todas las vistas: la función el(), que crea
// elementos del DOM de forma SEGURA.
//
// ¿Por qué no usar innerHTML con plantillas de texto?
//   Los datos vienen de la base de datos (placas, nombres, ubicaciones...). Si
//   alguien guarda "<img src=x onerror=...>" como nombre y lo metemos con
//   innerHTML, se ejecutaría código en el navegador (ataque XSS). Con el()
//   todo texto entra como TEXTO (textContent), nunca como HTML.

// el("button", { class: "boton", onclick: fn, "aria-label": "Cerrar" }, "Texto", otroNodo)
//   · atributos: "class", "texto" (textContent), "onclick"/"onsubmit"... (eventos), cualquier otro = atributo HTML.
//     Un valor false / null / undefined NO crea el atributo. Un valor true crea un atributo vacío (hidden, disabled).
//   · hijos: nodos o textos (se aceptan listas anidadas; null/false se ignoran).
export function el(etiqueta, atributos = {}, ...hijos) {
    const nodo = document.createElement(etiqueta);
    for (const [clave, valor] of Object.entries(atributos ?? {})) {
        if (valor === null || valor === undefined || valor === false) continue;
        if (clave === "class") nodo.className = valor;
        else if (clave === "texto") nodo.textContent = valor;
        else if (clave.startsWith("on") && typeof valor === "function") nodo.addEventListener(clave.slice(2).toLowerCase(), valor);
        else nodo.setAttribute(clave, valor === true ? "" : String(valor));
    }
    anexar(nodo, hijos);
    return nodo;
}

function anexar(nodo, hijos) {
    for (const hijo of hijos.flat(Infinity)) {
        if (hijo === null || hijo === undefined || hijo === false) continue;
        nodo.append(hijo instanceof Node ? hijo : document.createTextNode(String(hijo)));
    }
}

// Botón con el estilo de la app.  boton("Guardar", { variante: "primario", tipo: "submit" })
//   variante: "primario" (lima) | "contorno" | "peligro"      chico: versión compacta
export function boton(texto, { variante = "contorno", chico = false, tipo = "button", ...resto } = {}) {
    const clases = `boton boton--${variante}${chico ? " boton--chico" : ""}`;
    return el("button", { type: tipo, class: clases, ...resto }, texto);
}

// Etiqueta de color (chip). etiqueta("Habilitado", "ok")  tipos: ok | aviso | error | info | lima | (vacío = neutra)
export function etiqueta(texto, tipo = "") {
    return el("span", { class: tipo ? `etiqueta etiqueta--${tipo}` : "etiqueta" }, texto);
}

// Encabezado estándar de una vista:  sobretítulo + titular (con UNA palabra en serif lima) + descripción
//   encabezadoVista({ sobretitulo: "Parte 3", antes: "Flota de ", destacado: "vehículos", despues: "", descripcion: "…" })
// Genera el <h1>: "Flota de <em>vehículos</em>"
export function encabezadoVista({ sobretitulo, antes = "", destacado = "", despues = "", descripcion }) {
    return el("header", { class: "encabezado-vista" },
        sobretitulo ? el("p", { class: "sobretitulo" }, sobretitulo) : null,
        el("h1", {}, antes, destacado ? el("em", {}, destacado) : null, despues),
        descripcion ? el("p", {}, descripcion) : null);
}

// ---------------------------------- FECHAS -----------------------------------
// El backend manda las fechas como "2026-11-04" (LocalDate). Si se hiciera new Date("2026-11-04")
// el navegador la interpretaría en UTC y en Colombia (UTC-5) mostraría el 3 de noviembre.
// Por eso se arma la fecha a mano, en hora local.
function fechaLocal(texto) {
    const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(String(texto ?? ""));
    return m ? new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3])) : null;
}

// "2026-11-04" -> "4 nov 2026" (formato local de Colombia). Si no es una fecha válida devuelve "—".
export function formatearFecha(texto) {
    const fecha = fechaLocal(texto);
    return fecha ? fecha.toLocaleDateString("es-CO", { day: "numeric", month: "short", year: "numeric" }) : "—";
}

// Días que faltan desde hoy hasta la fecha (negativo = ya pasó). null si no es una fecha válida.
export function diasHasta(texto) {
    const fecha = fechaLocal(texto);
    if (!fecha) return null;
    const hoy = new Date();
    const inicioHoy = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate());
    return Math.round((fecha - inicioHoy) / 86400000);
}

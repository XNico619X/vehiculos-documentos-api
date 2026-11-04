// ============================================================================
// vistas/publico.js · PANTALLA PRINCIPAL (pública, sin sesión)
// ============================================================================
// Estructura (la del diseño pedido):
//   1. HERO: titular a la izquierda + visual grande a la derecha
//   2. Fila de datos pequeños (specs) separados por líneas verticales
//   3. Sección de CONSULTAS PÚBLICAS  <- la construye la Parte 1 (aquí solo queda el hueco)
//
// Los números de la fila de specs son reales: salen de las reglas del backend
// (6 consultas públicas, rutas de 2 a 7 paradas, tareas programadas cada 2 min y 90 s).

import { obtenerSesion } from "../session.js";
import { el } from "../ui/util.js";
import { montarConsultas } from "./consultas/consultasPublicas.js";
// Dibujo de la ruta del hero. Es un SVG ESTÁTICO escrito por nosotros (no contiene datos de
// usuarios), por eso es seguro usar innerHTML aquí. aria-hidden: es decorativo, el lector de
// pantalla lo salta. Las clases hv-* se estilizan en vistas.css.
const SVG_HERO = `
<svg viewBox="0 0 400 400" aria-hidden="true" focusable="false">
  <defs>
    <pattern id="hv-cuadricula" width="40" height="40" patternUnits="userSpaceOnUse">
      <path d="M40 0H0V40" fill="none" class="hv-rejilla"/>
    </pattern>
  </defs>
  <rect width="400" height="400" fill="url(#hv-cuadricula)"/>
  <path class="hv-ruta" d="M60 330 L140 255 L225 280 L290 175 L345 85"/>
  <g class="hv-parada hv-inicio" style="animation-delay:.5s"><circle cx="60" cy="330" r="15"/><text x="60" y="335">1</text></g>
  <g class="hv-parada hv-medio"  style="animation-delay:1.0s"><circle cx="140" cy="255" r="15"/><text x="140" y="260">2</text></g>
  <g class="hv-parada hv-medio"  style="animation-delay:1.4s"><circle cx="225" cy="280" r="15"/><text x="225" y="285">3</text></g>
  <g class="hv-parada hv-medio"  style="animation-delay:1.8s"><circle cx="290" cy="175" r="15"/><text x="290" y="180">4</text></g>
  <g class="hv-parada hv-fin"    style="animation-delay:2.2s"><circle cx="345" cy="85" r="15"/><text x="345" y="90">5</text></g>
</svg>`;

const SPECS = [
    { valor: "06", texto: "Consultas públicas" },
    { valor: "2–7", texto: "Paradas por ruta" },
    { valor: "2 min", texto: "Revisión de vencimientos" },
    { valor: "90 s", texto: "Coordenadas automáticas" },
];

export function montar(contenedor) {
    const haySesion = obtenerSesion() !== null;

    // ---- Texto del hero ----
    // Un solo <h1> por pantalla. La palabra destacada va en <em>: serif cursiva y color lima (base.css).
    const texto = el("div", { class: "hero-texto" },
        el("p", { class: "sobretitulo" }, "Plataforma de transporte"),
        el("h1", {}, "Vehículos y documentos, ", el("em", {}, "siempre"), " al día"),
        el("p", { class: "hero-apoyo" },
            "Consulta qué documentos están vencidos, qué conductores pueden operar y dónde está cada ruta. ",
            "Las consultas son abiertas; para registrar o modificar información inicia sesión."),
        el("div", { class: "hero-acciones" },
            // Botón principal: lima con texto negro
            el("button", {
                class: "boton boton--primario", type: "button",
                // scrollIntoView en lugar de un ancla (#consultas): un ancla cambiaría el hash y el router lo tomaría por una ruta
                onclick: () => document.getElementById("consultas-publicas")
                    ?.scrollIntoView({ behavior: "smooth", block: "start" }),
            }, "Ver consultas"),
            // Botón secundario: solo contorno
            el("a", { class: "boton boton--contorno", href: haySesion ? "#/mapa" : "#/login" },
                haySesion ? "Abrir el mapa" : "Iniciar sesión")));

    // ---- Visual del hero ----
    const visual = el("div", { class: "hero-visual" },
        el("div", { class: "hero-chip" },
            el("span", { class: "etiqueta-texto" }, "Ruta de ejemplo"),
            el("strong", {}, "5 paradas · 1 conductor")));
    visual.insertAdjacentHTML("afterbegin", SVG_HERO);

    // ---- Fila de specs ----
    const specs = el("ul", { class: "specs", "aria-label": "Datos del sistema" },
        SPECS.map((s) => el("li", { class: "spec" },
            el("span", { class: "spec-valor" }, s.valor),
            el("span", { class: "etiqueta-texto" }, s.texto))));

    // ---- Consultas públicas (Parte 1) ----
    // La sección lleva el id "consultas-publicas": a ella se desplaza el botón «Ver consultas» del hero.
    const consultas = el("section", { class: "seccion", id: "consultas-publicas", "aria-labelledby": "titulo-consultas" },
        el("div", { class: "encabezado-vista" },
            el("p", { class: "sobretitulo" }, "Sin iniciar sesión"),
            el("h2", { id: "titulo-consultas" }, "Consultas ", el("em", {}, "públicas")),
            el("p", {}, "Información abierta del sistema. No necesitas usuario ni APIKey.")));

    // montarConsultas dibuja las 6 tarjetas dentro de la sección y devuelve su función de limpieza,
    // que se pasa al router (return) para que se ejecute al salir de la pantalla.
    const limpiezaConsultas = montarConsultas(consultas);

    contenedor.append(el("section", { class: "hero" }, texto, visual), specs, consultas);
    return limpiezaConsultas;
}

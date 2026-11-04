// ============================================================================
// app.js · ARRANQUE DE LA APLICACIÓN
// ============================================================================
// Une las piezas de la carcasa (index.html):
//   · construye el MENÚ según haya o no sesión
//   · muestra en la cabecera el usuario y los botones "Iniciar / Cerrar sesión"
//   · reacciona a los cambios de sesión (token vencido, 401, cierre voluntario)
//   · arranca el router y, en cada cambio de pantalla: título, menú activo, foco y anuncio
//
// Las pantallas en sí NO están aquí: viven en js/vistas/ y las carga el router.

import { RUTAS, iniciarRouter, navegar, irALogin, rutaActual } from "./router.js";
import { obtenerSesion, obtenerUsuario, expiraEn, cerrarSesion, iniciarVigilancia } from "./session.js";
import { avisar, anunciar } from "./ui/mensajes.js";

const $ = (id) => document.getElementById(id);
const principal = $("contenido");
const listaMenu = $("menu-lista");
const botonMenu = $("menu-boton");
const menu = $("menu-principal");
const textoSesion = $("texto-sesion");
const botonEntrar = $("btn-entrar");
const botonSalir = $("btn-salir");

const NOMBRE_APP = "Transporte";
let navegaciones = 0; // cuántas pantallas se han mostrado (para NO robar el foco en la primera carga)

// ------------------------------- MENÚ ---------------------------------------
// Las pantallas protegidas solo aparecen si hay sesión: un visitante no ve opciones que no puede usar.
function construirMenu() {
    const haySesion = obtenerSesion() !== null;
    const items = RUTAS
        .filter((r) => r.menu && (!r.protegida || haySesion))
        .map((r) => {
            const li = document.createElement("li");
            const a = document.createElement("a");
            a.className = "menu-enlace";
            a.href = `#${r.ruta}`;
            a.textContent = r.etiqueta;     // textContent: nunca innerHTML
            a.dataset.ruta = r.ruta;
            li.append(a);
            return li;
        });
    listaMenu.replaceChildren(...items);
    marcarMenuActivo();
}

// aria-current="page": el lector de pantalla dice "página actual", y el CSS la resalta en lima
function marcarMenuActivo() {
    const ruta = rutaActual();
    // "/mapa/:codigo" debe resaltar la opción "/mapa": se compara con la parte fija de la ruta
    const base = ruta ? ruta.ruta.replace(/\/:.*$/, "") : null;
    for (const a of listaMenu.querySelectorAll("a")) {
        if (a.dataset.ruta === base) a.setAttribute("aria-current", "page");
        else a.removeAttribute("aria-current");
    }
}

// En pantallas pequeñas el menú se pliega detrás del botón "Menú"
function abrirMenu(abrir) {
    menu.classList.toggle("menu--abierto", abrir);
    botonMenu.setAttribute("aria-expanded", String(abrir));
}
botonMenu.addEventListener("click", () => abrirMenu(botonMenu.getAttribute("aria-expanded") !== "true"));
// Esc cierra el menú y devuelve el foco al botón
document.addEventListener("keydown", (e) => {
    if (e.key === "Escape" && botonMenu.getAttribute("aria-expanded") === "true") {
        abrirMenu(false);
        botonMenu.focus();
    }
});
// Elegir una opción cierra el menú plegado
menu.addEventListener("click", (e) => { if (e.target.closest("a")) abrirMenu(false); });

// --------------------------- CABECERA: SESIÓN -------------------------------
function actualizarCabeceraSesion() {
    const sesion = obtenerSesion();
    const usuario = obtenerUsuario();
    textoSesion.hidden = !sesion;
    botonSalir.hidden = !sesion;
    // "Iniciar sesión" sobra si ya hay sesión o si ya se está en la pantalla de login
    botonEntrar.hidden = Boolean(sesion) || rutaActual()?.ruta === "/login";
    if (sesion) {
        textoSesion.textContent = usuario ?? "Mi cuenta";
        const hora = expiraEn(sesion.token).toLocaleTimeString("es-CO", { hour: "2-digit", minute: "2-digit" });
        textoSesion.setAttribute("aria-label", `Mi cuenta, usuario ${usuario ?? ""}. Sesión válida hasta las ${hora}`);
        textoSesion.title = `Sesión válida hasta las ${hora}`;
    }
}

botonSalir.addEventListener("click", () => {
    cerrarSesion(); // dispara "sesion:cambio" -> abajo se avisa y se lleva al inicio
});

// Cualquier cambio de sesión (login, logout, vencimiento, 401) pasa por aquí
window.addEventListener("sesion:cambio", (evento) => {
    const { abierta, motivo } = evento.detail;
    construirMenu();
    actualizarCabeceraSesion();
    if (abierta) return; // el login ya sabe a dónde navegar

    const ruta = rutaActual();
    if (motivo) {
        // Cierre FORZADO (token vencido o 401). En el login no se avisa: el propio formulario ya muestra el error.
        if (ruta?.ruta !== "/login") avisar(motivo, "error");
        if (ruta?.protegida) irALogin(window.location.hash.slice(1).split("?")[0]);
    } else {
        // Cierre voluntario: aviso amable y de vuelta al inicio público
        avisar("Cerraste sesión.", "ok");
        navegar("/publico");
    }
});

// ------------------------ CADA CAMBIO DE PANTALLA ----------------------------
function alCambiarPantalla(ruta) {
    navegaciones++;
    // 1) Título de la pestaña (lo que se ve en el historial del navegador y lo que lee el lector de pantalla)
    document.title = `${ruta.titulo} · ${NOMBRE_APP}`;
    // 2) Menú y cabecera
    marcarMenuActivo();
    actualizarCabeceraSesion();
    abrirMenu(false);
    // 3) Llevar el foco al contenido nuevo y anunciar la pantalla. En la PRIMERA carga no se mueve el
    //    foco (el usuario aún no hizo nada), solo en navegaciones posteriores.
    if (navegaciones > 1) {
        window.scrollTo({ top: 0 });
        principal.focus({ preventScroll: true });
        anunciar(`Pantalla: ${ruta.titulo}`);
    }
}

// "Saltar al contenido": mueve el foco a <main> sin cambiar el hash (que es del router)
$("saltar-contenido").addEventListener("click", (e) => {
    e.preventDefault();
    principal.focus();
});

// ---------------------------------- ARRANQUE --------------------------------
// Si se recarga la página (F5) con sesión viva, hay que volver a programar el aviso de vencimiento
iniciarVigilancia();
construirMenu();
actualizarCabeceraSesion();
iniciarRouter({ contenedor: principal, alCambiar: alCambiarPantalla });

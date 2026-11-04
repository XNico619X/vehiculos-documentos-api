// ============================================================================
// session.js · CICLO DE VIDA DE LA SESIÓN (token + APIKey)
// ============================================================================
// Responsabilidades:
//   · guardar / leer / borrar la sesión (sessionStorage)
//   · saber cuándo vence el token (lo lee del propio JWT)
//   · AVISAR al resto de la app cuando la sesión empieza o termina, con un evento
//     del navegador:  window  "sesion:cambio"  { detail: { abierta, motivo } }
//     (app.js lo escucha para actualizar el menú y, si hace falta, mandar al login)
//
// Este archivo NO importa nada de la app: así api.js, router.js y las vistas
// pueden importarlo sin crear dependencias circulares.

const CLAVE = "sesionTransporte";
let temporizador = null;

// Intenta guardar; si el navegador lo impide (modo privado estricto, almacenamiento lleno)
// se avisa con una excepción clara en lugar de fallar en silencio.
export function guardarSesion(token, apikey) {
    // sessionStorage se borra al cerrar la pestaña. localStorage la dejaría guardada para siempre.
    sessionStorage.setItem(CLAVE, JSON.stringify({ token, apikey }));
    iniciarVigilancia();
    avisarCambio(true);
}

// Devuelve { token, apikey } si hay sesión VIGENTE; si no existe o ya venció, la limpia y devuelve null.
// Limpieza SILENCIOSA: el router ya sabe qué hacer cuando devuelve null (no hace falta un evento).
export function obtenerSesion() {
    const crudo = sessionStorage.getItem(CLAVE);
    if (!crudo) return null;
    try {
        const sesion = JSON.parse(crudo);
        if (!sesion.token || !sesion.apikey || haExpirado(sesion.token)) {
            cerrarSesion({ silencioso: true });
            return null;
        }
        return sesion;
    } catch {
        cerrarSesion({ silencioso: true });
        return null;
    }
}

// Cierra la sesión. Opciones:
//   { motivo: "texto" }  -> cierre FORZADO (token vencido, 401): app.js muestra el motivo y manda al login
//   { silencioso: true } -> limpieza interna (login fallido): no se avisa a nadie
//   (sin opciones)       -> el usuario pulsó "Cerrar sesión"
export function cerrarSesion({ motivo = null, silencioso = false } = {}) {
    const habiaSesion = sessionStorage.getItem(CLAVE) !== null;
    sessionStorage.removeItem(CLAVE);
    clearTimeout(temporizador);
    if (!silencioso && (habiaSesion || motivo)) avisarCambio(false, motivo);
}

// Sustituye SOLO la APIKey guardada (Parte 2: tras "regenerar API Key" la anterior deja de servir;
// si no se actualiza aquí, la siguiente petición respondería 401).
export function actualizarApikey(nuevaApikey) {
    const sesion = obtenerSesion();
    if (!sesion) return;
    sessionStorage.setItem(CLAVE, JSON.stringify({ token: sesion.token, apikey: nuevaApikey }));
}

// Notifica el cambio con un evento. Se dispara en una "microtarea" (queueMicrotask) para que
// el código que cerró la sesión termine primero y no haya reacciones en cadena a mitad de camino.
function avisarCambio(abierta, motivo = null) {
    queueMicrotask(() => window.dispatchEvent(
        new CustomEvent("sesion:cambio", { detail: { abierta, motivo } })));
}

// ---------------------------------------------------------------------------
// Lectura del JWT. Son 3 partes separadas por puntos: encabezado.payload.firma
// El payload (la del medio) es un JSON en Base64URL con, entre otros:
//   "sub" = el login del usuario     "exp" = instante de vencimiento en SEGUNDOS
// OJO: el front solo LEE estos datos para mostrarlos. Quien manda es el servidor:
// él valida la firma en cada petición.
// ---------------------------------------------------------------------------
function leerPayload(token) {
    try {
        const base64 = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/"); // Base64URL -> Base64
        return JSON.parse(atob(base64));
    } catch {
        return null;
    }
}

function leerVencimientoMs(token) {
    const exp = leerPayload(token)?.exp;
    return typeof exp === "number" ? exp * 1000 : 0; // a milisegundos, que es lo que usa Date. Ilegible = vencido
}

export function haExpirado(token) {
    return Date.now() >= leerVencimientoMs(token);
}

export function expiraEn(token) {
    return new Date(leerVencimientoMs(token));
}

// Login del usuario con sesión activa (para mostrarlo en la cabecera) o null
export function obtenerUsuario() {
    const sesion = obtenerSesion();
    return sesion ? (leerPayload(sesion.token)?.sub ?? null) : null;
}

// Programa un aviso para el instante exacto en que vence el token, aunque el usuario no toque nada.
// guardarSesion() la llama sola; app.js la llama al arrancar (por si se recargó la página con sesión viva).
export function iniciarVigilancia() {
    clearTimeout(temporizador);
    const crudo = sessionStorage.getItem(CLAVE);
    if (!crudo) return;
    let token;
    try { token = JSON.parse(crudo).token; } catch { return; }
    const faltan = leerVencimientoMs(token) - Date.now();
    // setTimeout no admite esperas de más de ~24 días; nuestro token dura 1 hora, así que sobra
    temporizador = setTimeout(
        () => cerrarSesion({ motivo: "Tu sesión expiró. Inicia sesión de nuevo." }),
        Math.max(faltan, 0));
}

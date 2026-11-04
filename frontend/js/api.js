// ============================================================================
// api.js · TODA la comunicación con el backend pasa por aquí
// ============================================================================
// Las vistas NUNCA llaman a fetch directamente: usan estas funciones.
//
//   apiPublico(ruta)            GET sin credenciales      (/api/publico/**, /api/auth/estado)
//   apiGet / apiPost / apiPut / apiDelete   rutas PROTEGIDAS: agregan solos los dos headers
//                               que exige el backend:  Authorization: Bearer <token>  y  X-API-Key
//   apiSubir(ruta, formData)    envía archivos (multipart/form-data)
//   login(usuario, password)    POST /api/auth/login
//
// Los errores se lanzan como ErrorApi, con:
//   .message   texto listo para mostrar al usuario
//   .estado    código HTTP (400, 404...) o 0 si no hubo respuesta
//   .detalles  lista de textos "campo: mensaje" (validación de @Valid), que
//              ui/formulario.js reparte junto a cada campo del formulario
//
// Si el servidor responde 401 (token vencido o APIKey inválida) se cierra la
// sesión y se lanza SesionExpiradaError; app.js lleva al usuario al login.

import { CONFIG } from "../config.js";
import { obtenerSesion, cerrarSesion } from "./session.js";

const TIEMPO_MAX_MS = 20000;         // una petición normal no debería tardar más
const TIEMPO_MAX_SUBIDA_MS = 60000;  // las subidas de archivos sí pueden tardar más

export class ErrorApi extends Error {
    constructor(mensaje, { estado = 0, detalles = [] } = {}) {
        super(mensaje);
        this.name = "ErrorApi";
        this.estado = estado;
        this.detalles = detalles;
    }
}

// Se lanza cuando el servidor dice 401: sirve para distinguir "se acabó la sesión" de otros fallos
export class SesionExpiradaError extends ErrorApi {
    constructor(mensaje) {
        super(mensaje, { estado: 401 });
        this.name = "SesionExpiradaError";
    }
}

// fetch solo lanza excepción cuando NO hay respuesta: servidor apagado, URL mal escrita, CORS o tiempo agotado
async function peticion(url, opciones, tiempoMax) {
    const controlador = new AbortController();
    const reloj = setTimeout(() => controlador.abort(), tiempoMax);
    try {
        return await fetch(url, { ...opciones, signal: controlador.signal });
    } catch (error) {
        if (error.name === "AbortError") {
            throw new ErrorApi("El servidor tardó demasiado en responder. Intenta de nuevo.");
        }
        throw new ErrorApi("No se pudo conectar con el servidor. Revisa que el backend esté encendido y que CORS esté configurado.");
    } finally {
        clearTimeout(reloj);
    }
}

// Nuestro backend responde los errores con { fecha, estado, mensaje, detalles }
async function leerError(respuesta) {
    let mensaje = `Error ${respuesta.status}`;
    let detalles = [];
    try {
        const cuerpo = await respuesta.json();
        mensaje = cuerpo.mensaje ?? mensaje;
        if (Array.isArray(cuerpo.detalles)) detalles = cuerpo.detalles;
    } catch { /* el cuerpo no era JSON: se queda el mensaje genérico */ }
    return { mensaje, detalles };
}

// Lee el cuerpo de una respuesta exitosa. Un DELETE puede devolver 204 SIN cuerpo
// (respuesta.json() fallaría), por eso se lee como texto y solo se convierte si hay algo.
async function leerCuerpo(respuesta) {
    if (respuesta.status === 204) return null;
    const texto = await respuesta.text();
    if (!texto) return null;
    try { return JSON.parse(texto); } catch { return texto; }
}

// Núcleo único: todas las funciones públicas de abajo terminan aquí.
//   cuerpo   -> objeto que se envía como JSON
//   archivos -> FormData (si se pasa, NO se fija Content-Type: el navegador agrega el "boundary")
//   conAuth  -> false para rutas públicas
async function solicitar(metodo, ruta, { cuerpo, archivos, conAuth = true } = {}) {
    const cabeceras = {};
    if (conAuth) {
        const sesion = obtenerSesion();
        if (!sesion) throw new SesionExpiradaError("Tu sesión expiró. Inicia sesión de nuevo.");
        cabeceras["Authorization"] = `Bearer ${sesion.token}`;
        cabeceras["X-API-Key"] = sesion.apikey;
    }

    const opciones = { method: metodo, headers: cabeceras };
    if (archivos) {
        opciones.body = archivos;
    } else if (cuerpo !== undefined) {
        cabeceras["Content-Type"] = "application/json";
        opciones.body = JSON.stringify(cuerpo);
    }

    const respuesta = await peticion(`${CONFIG.API_BASE_URL}${ruta}`, opciones,
        archivos ? TIEMPO_MAX_SUBIDA_MS : TIEMPO_MAX_MS);

    if (conAuth && respuesta.status === 401) { // token vencido o APIKey inválida: el servidor tiene la última palabra
        const { mensaje } = await leerError(respuesta);
        cerrarSesion({ motivo: mensaje });
        throw new SesionExpiradaError(mensaje);
    }
    if (!respuesta.ok) {
        const { mensaje, detalles } = await leerError(respuesta);
        throw new ErrorApi(mensaje, { estado: respuesta.status, detalles });
    }
    return leerCuerpo(respuesta);
}

// ---------------------------- API pública del módulo ------------------------

export const apiGet = (ruta) => solicitar("GET", ruta);
export const apiPost = (ruta, cuerpo) => solicitar("POST", ruta, { cuerpo });
export const apiPut = (ruta, cuerpo) => solicitar("PUT", ruta, { cuerpo });
export const apiDelete = (ruta) => solicitar("DELETE", ruta);

// Rutas SIN credenciales (consultas públicas, estado del sistema, creación del primer administrador)
export const apiPublico = (ruta) => solicitar("GET", ruta, { conAuth: false });
export const apiPublicoPost = (ruta, cuerpo) => solicitar("POST", ruta, { cuerpo, conAuth: false });

// Subida de archivos. `archivos` es un FormData: const f = new FormData(); f.append("archivo", file)
export const apiSubir = (ruta, archivos, metodo = "POST") => solicitar(metodo, ruta, { archivos });

// El login NO usa token (todavía no existe). Devuelve { token }.
export function login(usuario, password) {
    return solicitar("POST", "/api/auth/login", { cuerpo: { login: usuario, password }, conAuth: false });
}

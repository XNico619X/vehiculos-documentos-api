// ============================================================================
// vistas/cuenta.js · MI CUENTA  (#/cuenta)
// ============================================================================
// Se llega desde el nombre de usuario de la cabecera. Dos acciones sobre el usuario CON SESIÓN:
//   PUT /api/usuarios/{login}/password   { passwordActual, passwordNuevo }  -> 204
//   GET /api/usuarios/{login}/apikey     -> { login, apikey }  (genera una NUEVA y la anterior deja de servir)
// El backend solo permite cambiar las credenciales propias: {login} debe ser el del token.
//
// Detalle clave de la APIKey: en cuanto el servidor genera la nueva, la que está guardada en la
// sesión ya no sirve. Si no se reemplaza AL INSTANTE (actualizarApikey), la siguiente petición
// respondería 401 y la sesión se cerraría.

import { apiPut, apiGet, SesionExpiradaError } from "../api.js";
import { obtenerSesion, obtenerUsuario, expiraEn, actualizarApikey } from "../session.js";
import {
    crearCampo, enviarFormulario, datosFormulario, mostrarErrorCampo, mostrarErrorGeneral,
    agregarMostrarOcultar, ocultarValor,
} from "../ui/formulario.js";
import { confirmar } from "../ui/dialogo.js";
import { mostrarCredenciales } from "../ui/credenciales.js";
import { avisar, mostrarError } from "../ui/mensajes.js";
import { el, boton, encabezadoVista, etiqueta } from "../ui/util.js";

const MIN_PASSWORD = 8; // igual que @Size(min = 8) de CambioPasswordDTO

export function montar(contenedor) {
    const login = obtenerUsuario();
    const ruta = (accion) => `/api/usuarios/${encodeURIComponent(login)}/${accion}`;
    const sesion = obtenerSesion();
    const vence = sesion ? expiraEn(sesion.token).toLocaleTimeString("es-CO", { hour: "numeric", minute: "2-digit" }) : "—";

    // ------------------------- Cambiar contraseña ---------------------------
    const actual = crearCampo({
        nombre: "passwordActual", etiqueta: "Contraseña actual", tipo: "password", requerido: true,
        atributos: { autocomplete: "current-password" },
    });
    const nueva = crearCampo({
        nombre: "passwordNuevo", etiqueta: "Contraseña nueva", tipo: "password", requerido: true,
        ayuda: `Mínimo ${MIN_PASSWORD} caracteres y distinta de la actual.`,
        atributos: { autocomplete: "new-password", minlength: String(MIN_PASSWORD) },
    });
    const confirmacion = crearCampo({
        nombre: "confirmacion", etiqueta: "Confirma la contraseña nueva", tipo: "password", requerido: true,
        atributos: { autocomplete: "new-password" },
    });
    [actual, nueva, confirmacion].forEach((c) => agregarMostrarOcultar(c.entrada));

    const formPassword = el("form", { class: "formulario", "aria-labelledby": "titulo-password" },
        actual.contenedor, nueva.contenedor, confirmacion.contenedor,
        el("div", { class: "formulario-acciones" }, boton("Cambiar contraseña", { variante: "primario", tipo: "submit" })));

    enviarFormulario(formPassword, {
        textoCargando: "Guardando…",
        alEnviar: async (form) => {
            const datos = datosFormulario(form);
            // minlength del HTML solo se revisa cuando el usuario ESCRIBE: un valor pegado por un gestor de
            // contraseñas o puesto por código lo salta. Por eso se comprueba aquí también.
            if (datos.passwordNuevo.length < MIN_PASSWORD) {
                mostrarErrorCampo(nueva.entrada, `Escribe al menos ${MIN_PASSWORD} caracteres.`);
                nueva.entrada.focus();
                return;
            }
            // Reglas que el navegador no puede revisar solo (comparar dos campos)
            if (datos.passwordNuevo === datos.passwordActual) {
                mostrarErrorCampo(nueva.entrada, "La contraseña nueva debe ser distinta de la actual.");
                nueva.entrada.focus();
                return;
            }
            if (datos.passwordNuevo !== datos.confirmacion) {
                mostrarErrorCampo(confirmacion.entrada, "No coincide con la contraseña nueva.");
                confirmacion.entrada.focus();
                return;
            }
            try {
                await apiPut(ruta("password"), { passwordActual: datos.passwordActual, passwordNuevo: datos.passwordNuevo });
            } catch (error) {
                // 400 "El password actual ingresado es incorrecto." -> se muestra junto al campo que está mal
                if (error.estado === 400 && /actual/i.test(error.message) && !error.detalles?.length) {
                    mostrarErrorCampo(actual.entrada, "La contraseña actual no es correcta.");
                    actual.entrada.value = "";
                    actual.entrada.focus();
                    return;
                }
                throw error; // el resto (validación por campo, 403...) lo reparte enviarFormulario
            }
            form.reset();
            [actual, nueva, confirmacion].forEach((c) => ocultarValor(c.entrada));
            avisar("Contraseña cambiada. Úsala la próxima vez que inicies sesión.", "ok");
        },
    });

    // ---------------------------- Regenerar APIKey ---------------------------
    const botonRegenerar = boton("Regenerar APIKey", { variante: "peligro" });
    const estadoApikey = el("p", { class: "mensaje", role: "status" });
    botonRegenerar.addEventListener("click", async () => {
        if (botonRegenerar.getAttribute("aria-busy") === "true") return;
        const ok = await confirmar({
            titulo: "¿Regenerar la APIKey?",
            texto: "La APIKey actual dejará de funcionar de inmediato. Tendrás que usar la nueva para iniciar sesión " +
                "y en cualquier herramienta donde la tengas guardada (por ejemplo, Postman).",
            textoConfirmar: "Sí, regenerar", peligro: true,
        });
        if (!ok) return;

        botonRegenerar.setAttribute("aria-busy", "true");
        botonRegenerar.textContent = "Regenerando…";
        try {
            const { apikey } = await apiGet(ruta("apikey"));
            if (!apikey) throw new Error("El servidor no devolvió la APIKey nueva.");
            actualizarApikey(apikey); // ANTES de cualquier otra petición: la anterior ya no sirve
            estadoApikey.textContent = "";
            await mostrarCredenciales({
                titulo: "Tu nueva APIKey",
                texto: "Ya quedó activa en esta sesión. Guárdala ahora: es la única vez que se muestra.",
                apikey,
            });
            avisar("APIKey regenerada.", "ok");
        } catch (error) {
            if (error instanceof SesionExpiradaError) return;
            mostrarError(error);
        } finally {
            if (botonRegenerar.isConnected) {
                botonRegenerar.removeAttribute("aria-busy");
                botonRegenerar.textContent = "Regenerar APIKey";
                botonRegenerar.focus();
            }
        }
    });

    contenedor.append(
        encabezadoVista({
            sobretitulo: "Sesión", antes: "Mi ", destacado: "cuenta",
            descripcion: "Cambia tu contraseña o genera una APIKey nueva si crees que alguien conoce la actual.",
        }),
        el("div", { class: "tarjeta cuenta-resumen" },
            el("dl", { class: "datos" },
                el("div", {}, el("dt", { class: "etiqueta-texto" }, "Usuario"), el("dd", { class: "mono" }, login ?? "—")),
                el("div", {}, el("dt", { class: "etiqueta-texto" }, "Sesión válida hasta"), el("dd", {}, vence)),
                el("div", {}, el("dt", { class: "etiqueta-texto" }, "Perfil"), el("dd", {}, etiqueta("Administrativo", "lima"))))),
        el("div", { class: "cuenta-layout" },
            el("section", { class: "tarjeta", "aria-labelledby": "titulo-password" },
                el("h2", { id: "titulo-password", class: "titulo-seccion" }, "Cambiar contraseña"),
                formPassword),
            el("section", { class: "tarjeta", "aria-labelledby": "titulo-apikey" },
                el("h2", { id: "titulo-apikey", class: "titulo-seccion" }, "APIKey"),
                el("p", { class: "texto-tenue" },
                    "La APIKey viaja en el encabezado X-API-Key de cada petición. Por seguridad no se puede volver a ver: ",
                    "si la perdiste, genera una nueva."),
                el("div", { class: "formulario-acciones" }, botonRegenerar),
                estadoApikey)));

    actual.entrada.focus({ preventScroll: true });
}

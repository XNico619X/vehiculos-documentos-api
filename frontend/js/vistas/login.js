// ============================================================================
// vistas/login.js · INICIO DE SESIÓN  +  PRIMER ADMINISTRADOR
// ============================================================================
// Al abrir, pregunta al backend GET /api/auth/estado (público):
//   · { requiereConfiguracionInicial: true }  -> el sistema no tiene usuarios todavía: se muestra
//     el formulario "Crear el primer administrador" (POST /api/auth/configuracion-inicial, público).
//     Las credenciales que devuelve se muestran UNA sola vez y luego se pasa al login.
//   · false (o si la consulta falla)          -> se muestra el login normal.
//
// Login en dos pasos, para poder decir EXACTAMENTE qué estuvo mal:
//   1. POST /api/auth/login { login, password } -> { token }. Si falla: "usuario o contraseña incorrectos".
//   2. Se guarda la sesión y se comprueba la APIKey con una consulta protegida pequeña
//      (GET /api/documentos). Si responde 401, el error es de la APIKey, no de la contraseña.

import { login, apiGet, apiPublico, apiPublicoPost } from "../api.js";
import { guardarSesion, cerrarSesion } from "../session.js";
import {
    crearCampo, enviarFormulario, mostrarErrorGeneral, mostrarErrorCampo, datosFormulario, agregarMostrarOcultar,
} from "../ui/formulario.js";
import { mostrarCredenciales } from "../ui/credenciales.js";
import { avisar } from "../ui/mensajes.js";
import { el, boton, encabezadoVista } from "../ui/util.js";
import { DESTINO_TRAS_LOGIN } from "../router.js";

export async function montar(contenedor, { consulta, navegar }) {
    // El router manda a /login?volver=/vehiculos cuando alguien intentó abrir una pantalla protegida
    const volver = consulta.get("volver");
    const destino = volver && volver.startsWith("/") && !volver.startsWith("//") ? volver : DESTINO_TRAS_LOGIN;

    let requiereConfiguracion = false;
    try {
        ({ requiereConfiguracionInicial: requiereConfiguracion } = await apiPublico("/api/auth/estado"));
    } catch {
        // Backend apagado o sin este servicio: se muestra el login, que dará el error de conexión al enviar
    }
    if (!contenedor.isConnected) return; // el usuario ya se fue a otra pantalla mientras se consultaba

    if (requiereConfiguracion) montarPrimerAdministrador();
    else montarLogin();

    // ------------------------------- LOGIN ----------------------------------
    function montarLogin({ usuarioSugerido = "" } = {}) {
        const campoUsuario = crearCampo({
            nombre: "login", etiqueta: "Usuario", requerido: true, valor: usuarioSugerido,
            atributos: { autocomplete: "username", autocapitalize: "none", spellcheck: "false" },
        });
        const campoPassword = crearCampo({
            nombre: "password", etiqueta: "Contraseña", tipo: "password", requerido: true,
            atributos: { autocomplete: "current-password" },
        });
        const campoApikey = crearCampo({
            nombre: "apikey", etiqueta: "APIKey", tipo: "password", requerido: true,
            ayuda: "La APIKey se muestra una sola vez, al crear tu usuario. Si la perdiste, regenérala desde «Mi cuenta».",
            atributos: { autocomplete: "off", spellcheck: "false" },
        });
        agregarMostrarOcultar(campoPassword.entrada);
        agregarMostrarOcultar(campoApikey.entrada);

        const formulario = el("form", { class: "formulario", id: "form-login" },
            campoUsuario.contenedor, campoPassword.contenedor, campoApikey.contenedor,
            el("div", { class: "formulario-acciones" },
                boton("Entrar", { variante: "primario", tipo: "submit" })));

        // Si lo mandaron aquí desde una pantalla protegida, se explica por qué (role="status": se anuncia)
        if (volver) formulario.prepend(el("p", { class: "mensaje", role: "status" }, "Inicia sesión para continuar."));

        enviarFormulario(formulario, {
            textoCargando: "Entrando…",
            alEnviar: async (form) => {
                const datos = datosFormulario(form);

                // Paso 1: usuario y contraseña
                let token;
                try {
                    ({ token } = await login(datos.login, datos.password));
                    if (!token) throw new Error("El servidor no devolvió el token.");
                } catch (error) {
                    // El backend responde 404 "Login o password incorrectos" (no dice cuál de los dos, a propósito)
                    const credencialesMalas = error.estado === 404 || error.estado === 400 || error.estado === 401;
                    mostrarErrorGeneral(form, credencialesMalas
                        ? "Usuario o contraseña incorrectos. Revisa los dos datos e intenta de nuevo."
                        : (error.message || "No se pudo iniciar sesión."));
                    if (credencialesMalas) {
                        campoPassword.entrada.value = "";
                        campoPassword.entrada.focus();
                    }
                    return;
                }

                // Paso 2: la APIKey (el login no la comprueba; la primera consulta protegida sí)
                guardarSesion(token, datos.apikey);
                try {
                    await apiGet("/api/documentos");
                } catch (error) {
                    cerrarSesion({ silencioso: true }); // no dejar una sesión a medias con una clave mala
                    campoApikey.entrada.value = "";
                    if (error.estado === 401) {
                        mostrarErrorGeneral(form, "Tu usuario y contraseña son correctos, pero la APIKey no corresponde a este usuario.");
                        mostrarErrorCampo(campoApikey.entrada, "APIKey inválida. Pega la que se mostró al crear tu usuario.");
                        campoApikey.entrada.focus();
                    } else {
                        mostrarErrorGeneral(form, error.message || "No se pudo comprobar la APIKey.");
                    }
                    return;
                }

                // Éxito: ni la contraseña ni la clave se quedan en el formulario
                campoPassword.entrada.value = "";
                campoApikey.entrada.value = "";
                navegar(destino, { reemplazar: true }); // "Atrás" no vuelve al login
            },
        });

        contenedor.replaceChildren(el("section", { class: "login" },
            encabezadoVista({
                sobretitulo: "Acceso de administradores", antes: "Iniciar ", destacado: "sesión",
                descripcion: "Entra con tu usuario, tu contraseña y tu APIKey.",
            }),
            el("div", { class: "tarjeta" }, formulario)));

        (usuarioSugerido ? campoPassword : campoUsuario).entrada.focus(); // el cursor queda listo para escribir
    }

    // ------------------------- PRIMER ADMINISTRADOR -------------------------
    function montarPrimerAdministrador() {
        const campos = [
            crearCampo({ nombre: "nombres", etiqueta: "Nombres", requerido: true, atributos: { autocomplete: "given-name" } }),
            crearCampo({ nombre: "apellidos", etiqueta: "Apellidos", requerido: true, atributos: { autocomplete: "family-name" } }),
            crearCampo({
                nombre: "tipoIdentificacion", etiqueta: "Tipo de identificación", tipo: "select", requerido: true, valor: "CC",
                opciones: [{ valor: "CC", texto: "Cédula de ciudadanía" }],
            }),
            crearCampo({
                nombre: "identificacion", etiqueta: "Número de identificación", requerido: true,
                ayuda: "Solo números. Con ella y tus iniciales se forma tu usuario.",
                atributos: { inputmode: "numeric", pattern: "\\d{5,15}", title: "Escribe entre 5 y 15 dígitos, sin puntos ni espacios." },
            }),
            crearCampo({
                nombre: "correoElectronico", etiqueta: "Correo electrónico", tipo: "email", requerido: true,
                atributos: { autocomplete: "email" },
            }),
        ];

        const formulario = el("form", { class: "formulario", id: "form-primer-admin" },
            el("div", { class: "formulario-fila" }, campos[0].contenedor, campos[1].contenedor),
            el("div", { class: "formulario-fila" }, campos[2].contenedor, campos[3].contenedor),
            campos[4].contenedor,
            el("div", { class: "formulario-acciones" },
                boton("Crear administrador", { variante: "primario", tipo: "submit" })));

        enviarFormulario(formulario, {
            textoCargando: "Creando…",
            alEnviar: async (form) => {
                // tipoPersona lo exige el DTO; el backend igual fuerza ADMINISTRATIVO
                const persona = await apiPublicoPost("/api/auth/configuracion-inicial", { ...datosFormulario(form), tipoPersona: "A" });
                const usuario = persona?.usuario ?? {};
                await mostrarCredenciales({
                    titulo: "Administrador creado",
                    texto: "Guarda estos tres datos ahora: es la única vez que se muestran. Los necesitas para iniciar sesión.",
                    login: usuario.login, password: usuario.password, apikey: usuario.apikey,
                });
                avisar("Listo. Ahora inicia sesión con tus credenciales.", "ok");
                montarLogin({ usuarioSugerido: usuario.login ?? "" });
            },
        });

        contenedor.replaceChildren(el("section", { class: "login" },
            encabezadoVista({
                sobretitulo: "Configuración inicial", antes: "Primer ", destacado: "administrador",
                descripcion: "El sistema todavía no tiene usuarios. Crea el primer administrador: el usuario, la contraseña y la APIKey se generan solos.",
            }),
            el("div", { class: "tarjeta" }, formulario)));

        campos[0].entrada.focus();
    }
}

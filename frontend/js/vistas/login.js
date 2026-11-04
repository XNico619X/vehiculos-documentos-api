// ============================================================================
// vistas/login.js · INICIO DE SESIÓN
// ============================================================================
// Es el mismo flujo que ya funcionaba, ahora como una pantalla más del router:
//   1. POST /api/auth/login  { login, password }  ->  { token }
//   2. La APIKey NO la devuelve el login: la escribe el usuario (se mostró UNA sola vez al crear su usuario)
//   3. Se guarda la sesión y se comprueba YA la APIKey con una consulta protegida pequeña
//      (GET /api/documentos). Si es incorrecta el servidor responde 401 y se avisa aquí,
//      no más adelante con una pantalla rota.
//
// (Parte 2 agregará: mostrar/ocultar contraseña y el formulario de "Primer administrador".)

import { login, apiGet } from "../api.js";
import { guardarSesion, cerrarSesion } from "../session.js";
import { crearCampo, enviarFormulario, mostrarErrorGeneral, datosFormulario } from "../ui/formulario.js";
import { el, boton, encabezadoVista } from "../ui/util.js";
import { DESTINO_TRAS_LOGIN } from "../router.js";

export function montar(contenedor, { consulta, navegar }) {
    // El router manda a /login?volver=/vehiculos cuando alguien intentó abrir una pantalla protegida
    const volver = consulta.get("volver");
    const destino = volver && volver.startsWith("/") && !volver.startsWith("//") ? volver : DESTINO_TRAS_LOGIN;

    const campoUsuario = crearCampo({
        nombre: "login", etiqueta: "Usuario", requerido: true,
        atributos: { autocomplete: "username", autocapitalize: "none", spellcheck: "false" },
    });
    const campoPassword = crearCampo({
        nombre: "password", etiqueta: "Contraseña", tipo: "password", requerido: true,
        atributos: { autocomplete: "current-password" },
    });
    const campoApikey = crearCampo({
        nombre: "apikey", etiqueta: "APIKey", tipo: "password", requerido: true,
        ayuda: "La APIKey se muestra una sola vez, al crear tu usuario. Guárdala en un lugar seguro.",
        atributos: { autocomplete: "off", spellcheck: "false" },
    });

    const formulario = el("form", { class: "formulario", id: "form-login" },
        campoUsuario.contenedor, campoPassword.contenedor, campoApikey.contenedor,
        el("div", { class: "formulario-acciones" },
            boton("Entrar", { variante: "primario", tipo: "submit" })));

    // Si lo mandaron aquí desde una pantalla protegida, se explica por qué (role="status": se anuncia)
    if (volver) {
        formulario.prepend(el("p", { class: "mensaje", role: "status" }, "Inicia sesión para continuar."));
    }

    enviarFormulario(formulario, {
        textoCargando: "Entrando…",
        alEnviar: async (form) => {
            const datos = datosFormulario(form);
            try {
                const { token } = await login(datos.login, datos.password);
                if (!token) throw new Error("El servidor no devolvió el token.");
                guardarSesion(token, datos.apikey);   // la APIKey la escribió el usuario
                await apiGet("/api/documentos");       // comprueba la APIKey YA (401 si es inválida)
            } catch (error) {
                cerrarSesion({ silencioso: true });    // no dejar una sesión a medias (token guardado con una clave mala)
                // En vez de dejar que enviarFormulario lo trate como error de campo, se muestra arriba del formulario
                mostrarErrorGeneral(form, error.message || "No se pudo iniciar sesión.");
                campoApikey.entrada.value = "";        // la clave no debe quedarse en el formulario tras un fallo
                return;
            }
            // Éxito: ni la contraseña ni la clave se quedan en el formulario
            campoPassword.entrada.value = "";
            campoApikey.entrada.value = "";
            navegar(destino, { reemplazar: true });    // "Atrás" no vuelve al login
        },
    });

    contenedor.append(el("section", { class: "login" },
        encabezadoVista({
            sobretitulo: "Acceso de administradores", antes: "Iniciar ", destacado: "sesión",
            descripcion: "Entra con tu usuario, tu contraseña y tu APIKey.",
        }),
        el("div", { class: "tarjeta" }, formulario)));

    campoUsuario.entrada.focus(); // el cursor queda listo para escribir
}

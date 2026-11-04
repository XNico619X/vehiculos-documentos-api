package com.utolima.vehiculosdocumentosapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.utolima.vehiculosdocumentosapi.dto.CambioPasswordDTO;
import com.utolima.vehiculosdocumentosapi.dto.UsuarioResponseDTO;
import com.utolima.vehiculosdocumentosapi.model.Usuario;
import com.utolima.vehiculosdocumentosapi.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    // El login del usuario se envía por la URL.
    // 204 sin cuerpo: antes se devolvía la entidad Usuario completa, con la contraseña en texto plano.
    @PutMapping("/{login}/password")
    public ResponseEntity<Void> cambiarPassword(
            @PathVariable String login,
            @Valid @RequestBody CambioPasswordDTO dto,
            @AuthenticationPrincipal Usuario autenticado) {
        exigirMismoUsuario(login, autenticado);
        usuarioService.cambiarPassword(login, dto);
        return ResponseEntity.noContent().build();
    }

    // Servicio GET que permite generar nuevamente la APIKey.
    // Devuelve SOLO { login, apikey }: el front la necesita para actualizar la sesión de inmediato
    // (la anterior deja de servir). La entidad no se devuelve: traía la contraseña y una Persona LAZY.
    @GetMapping("/{login}/apikey")
    public ResponseEntity<UsuarioResponseDTO> regenerarApiKey(
            @PathVariable String login,
            @AuthenticationPrincipal Usuario autenticado) {
        exigirMismoUsuario(login, autenticado);
        Usuario usuarioActualizado = usuarioService.regenerarApiKey(login);
        UsuarioResponseDTO respuesta = new UsuarioResponseDTO();
        respuesta.setLogin(usuarioActualizado.getId().getLogin());
        respuesta.setApikey(usuarioActualizado.getApikey());
        return ResponseEntity.ok(respuesta);
    }

    // Cada quien solo cambia SU contraseña y SU APIKey. Sin esta regla, cualquier usuario con sesión
    // podía cambiar las credenciales de otro con solo poner su login en la URL.
    // El principal es el Usuario que JwtAuthenticationFilter puso en el contexto de seguridad.
    private void exigirMismoUsuario(String login, Usuario autenticado) {
        if (autenticado == null || !autenticado.getId().getLogin().equals(login)) {
            throw new AccessDeniedException("Solo puedes cambiar las credenciales de tu propio usuario.");
        }
    }
}

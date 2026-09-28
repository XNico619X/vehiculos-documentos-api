package com.utolima.vehiculosdocumentosapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.utolima.vehiculosdocumentosapi.dto.CambioPasswordDTO;
import com.utolima.vehiculosdocumentosapi.model.Usuario;
import com.utolima.vehiculosdocumentosapi.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    // El login del usuario se envié por la URL
    @PutMapping("/{login}/password")
    public ResponseEntity<Usuario> cambiarPassword(
            @PathVariable String login, 
            @Valid @RequestBody CambioPasswordDTO dto) {
        Usuario usuarioActualizado = usuarioService.cambiarPassword(login, dto);
        return ResponseEntity.ok(usuarioActualizado);
    }

    // Servicio GET que permita nuevamente la generación del APIKey
    @GetMapping("/{login}/apikey")
    public ResponseEntity<Usuario> regenerarApiKey(@PathVariable String login) {
        Usuario usuarioActualizado = usuarioService.regenerarApiKey(login);
        return ResponseEntity.ok(usuarioActualizado);
    }
}
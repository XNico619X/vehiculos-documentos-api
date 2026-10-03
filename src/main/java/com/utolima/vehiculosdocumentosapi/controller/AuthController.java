package com.utolima.vehiculosdocumentosapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.utolima.vehiculosdocumentosapi.dto.LoginRequestDTO;
import com.utolima.vehiculosdocumentosapi.dto.LoginResponseDTO;
import com.utolima.vehiculosdocumentosapi.exception.RecursoNoEncontradoException;
import com.utolima.vehiculosdocumentosapi.model.Usuario;
import com.utolima.vehiculosdocumentosapi.repository.UsuarioRepository;
import com.utolima.vehiculosdocumentosapi.security.JwtUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth") // este SI va a quedar publico (sin token), tiene sentido: nadie tiene token antes de loguearse
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        Usuario usuario = usuarioRepository.findByIdLogin(dto.getLogin())
                .orElseThrow(() -> new RecursoNoEncontradoException("Login o password incorrectos"));

        // comparacion de password en texto plano,mismo nivel que ya maneja UsuarioServiceImpl.cambiarPassword de Karen.
        // Nota: en un proyecto real esto NUNCA se hace asi, las contrasenas se guardan hasheadas (ej. BCrypt).
        if (!usuario.getPassword().equals(dto.getPassword())) {
            throw new RecursoNoEncontradoException("Login o password incorrectos");
        }

        String token = jwtUtil.generarToken(usuario.getId().getLogin());
        return ResponseEntity.ok(new LoginResponseDTO(token));
    }
}
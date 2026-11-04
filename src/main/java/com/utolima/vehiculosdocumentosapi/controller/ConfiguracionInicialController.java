package com.utolima.vehiculosdocumentosapi.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.utolima.vehiculosdocumentosapi.dto.EstadoSistemaDTO;
import com.utolima.vehiculosdocumentosapi.dto.PersonaRequestDTO;
import com.utolima.vehiculosdocumentosapi.dto.PersonaResponseDTO;
import com.utolima.vehiculosdocumentosapi.service.ConfiguracionInicialService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Estas dos rutas son PUBLICAS (sin token) porque /api/auth/** ya esta en permitAll de SecurityConfig.
// Es intencional: quien instala el sistema todavia no tiene ningun usuario con el que autenticarse.
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class ConfiguracionInicialController {

    private final ConfiguracionInicialService configuracionInicialService;

    // El front la consulta al abrir la pantalla de inicio de sesion para decidir si muestra "Crear primer administrador".
    @GetMapping("/estado")
    public ResponseEntity<EstadoSistemaDTO> estado() {
        return ResponseEntity.ok(new EstadoSistemaDTO(configuracionInicialService.requiereConfiguracionInicial()));
    }

    // Mismo cuerpo que POST /api/personas. El tipoPersona que envien se ignora: siempre se crea un ADMINISTRATIVO.
    // Si el DTO exige tipoPersona, el front manda "A".
    @PostMapping("/configuracion-inicial")
    public ResponseEntity<PersonaResponseDTO> crearPrimerAdministrador(@Valid @RequestBody PersonaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(configuracionInicialService.crearPrimerAdministrador(dto));
    }
}
package com.utolima.vehiculosdocumentosapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.utolima.vehiculosdocumentosapi.dto.CargueResponseDTO;
import com.utolima.vehiculosdocumentosapi.service.CargueConsultaService;

import lombok.RequiredArgsConstructor;

// Controlador APARTE a propósito: TrayectoController es un archivo compartido y editarlo genera conflictos.
// La ruta empieza por /api/trayectos/, así que SecurityConfig la deja PROTEGIDA (token + APIKey) sin tocar nada.
@RestController
@RequestMapping("/api/trayectos/cargues")
@RequiredArgsConstructor
public class CargueConsultaController {

    private final CargueConsultaService cargueConsultaService;

    // Consulta de un cargue: @PathVariable toma {idCargue} de la URL (ej. 202610231530).
    // Es Long porque 12 dígitos no caben en Integer.
    @GetMapping("/{idCargue}")
    public ResponseEntity<CargueResponseDTO> consultarCargue(@PathVariable Long idCargue) {
        return ResponseEntity.ok(cargueConsultaService.consultarCargue(idCargue));
    }
}
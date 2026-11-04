package com.utolima.vehiculosdocumentosapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.utolima.vehiculosdocumentosapi.dto.CambioEstadoDocumentoDTO;
import com.utolima.vehiculosdocumentosapi.dto.DocumentoVehiculoEstadoResponseDTO;
import com.utolima.vehiculosdocumentosapi.service.VehiculoDocumentoEstadoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Controlador APARTE a proposito (como VehiculoPersonaController): no se edita VehiculoController.
// La ruta empieza por /api/vehiculos/, asi que SecurityConfig la deja PROTEGIDA (token + APIKey) sin tocar nada.
@RestController
@RequestMapping("/api/vehiculos")
@RequiredArgsConstructor
public class VehiculoDocumentoEstadoController {

    private final VehiculoDocumentoEstadoService vehiculoDocumentoEstadoService;

    // PUT /api/vehiculos/{idVehiculo}/documentos/{idDocumento}/estado
    // @Valid hace que se compruebe el @NotNull del DTO; si falla, el manejador global responde 400.
    @PutMapping("/{idVehiculo}/documentos/{idDocumento}/estado")
    public ResponseEntity<DocumentoVehiculoEstadoResponseDTO> cambiarEstado(
            @PathVariable Long idVehiculo,
            @PathVariable Long idDocumento,
            @Valid @RequestBody CambioEstadoDocumentoDTO dto) {
        return ResponseEntity.ok(vehiculoDocumentoEstadoService.cambiarEstado(idVehiculo, idDocumento, dto));
    }
}
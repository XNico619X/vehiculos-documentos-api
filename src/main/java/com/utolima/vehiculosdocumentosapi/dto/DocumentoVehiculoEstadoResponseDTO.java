package com.utolima.vehiculosdocumentosapi.dto;

import java.time.LocalDate;

import com.utolima.vehiculosdocumentosapi.model.enums.EstadoDocumento;

// Respuesta corta: como quedo el documento despues del cambio.
// Es un DTO (record) porque nunca devolvemos la entidad VehiculoDocumento: trae el PDF en Base64 y relaciones LAZY.
public record DocumentoVehiculoEstadoResponseDTO(
        Long idVehiculo,
        Long idDocumento,
        EstadoDocumento estadoDocumento,
        LocalDate fechaVencimiento) {
}
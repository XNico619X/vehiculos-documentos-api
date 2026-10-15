package com.utolima.vehiculosdocumentosapi.dto;

import java.math.BigDecimal;

// Una parada (trayecto) tal como la verá quien consuma la API.
// Reemplaza a la entidad Trayecto, cuyas relaciones LAZY romperían el JSON.
public record TrayectoResponseDTO(
        Long idTrayecto,
        Integer ordenParada,           // 0 = parada inicial; el mayor = parada final
        String ubicacion,              // texto, ej: "Conservatorio del Tolima, Ibagué"
        BigDecimal latitud,            // puede ser null hasta que el scheduler consulte Google
        BigDecimal longitud,
        ConductorResumenDTO conductor, // DTO anidado en vez de la entidad Persona
        VehiculoResumenDTO vehiculo,   // DTO anidado en vez de la entidad Vehiculo
        String loginUsuario) {         // usuario que registró el trayecto
}
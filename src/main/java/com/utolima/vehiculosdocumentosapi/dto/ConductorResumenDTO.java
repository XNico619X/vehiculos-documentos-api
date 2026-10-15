package com.utolima.vehiculosdocumentosapi.dto;

// "record" = clase inmutable: Java genera constructor, getters y toString solos.
// Este DTO muestra SOLO los datos públicos del conductor.
// No incluye correo ni licencia: así no exponemos información sensible.
public record ConductorResumenDTO(
        Long id,               // id de la tabla persona (campo idPersona de la entidad)
        String identificacion, // número de documento de identidad
        String nombres,
        String apellidos) {
}
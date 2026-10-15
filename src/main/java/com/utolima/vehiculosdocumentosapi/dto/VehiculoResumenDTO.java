package com.utolima.vehiculosdocumentosapi.dto;

// Resumen del vehículo: solo lo que pide el enunciado (id y placa).
// Evita devolver la entidad Vehiculo completa y su lista perezosa de documentos.
public record VehiculoResumenDTO(
        Long id,      // campo "id" de la entidad Vehiculo
        String placa) {
}
package com.utolima.vehiculosdocumentosapi.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ErrorResponseDTO {
    private LocalDateTime fecha;   // cuando ocurrio el error
    private int estado;            // el codigo HTTP (404, 400, etc.), duplicado del header pero util en el body
    private String mensaje;        // el mensaje principal del error
    private List<String> detalles; // para validaciones: lista de "campo X es obligatorio", etc. Puede venir null
}
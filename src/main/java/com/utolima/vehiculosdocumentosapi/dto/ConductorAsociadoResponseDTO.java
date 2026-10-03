package com.utolima.vehiculosdocumentosapi.dto;

import java.time.LocalDate;

import com.utolima.vehiculosdocumentosapi.model.enums.EstadoConductor;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ConductorAsociadoResponseDTO {
    private Long idPersona;
    private String identificacion;
    private String nombres;
    private String apellidos;
    private LocalDate fechaAsociacion;
    private EstadoConductor estadoConductor;
    // sin referencia de vuelta al vehiculo, mismo principio anti-recursion que usamos en DocumentoAsociadoResponseDTO
}
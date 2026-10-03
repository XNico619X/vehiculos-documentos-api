package com.utolima.vehiculosdocumentosapi.dto;

import com.utolima.vehiculosdocumentosapi.model.enums.EstadoConductor;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CambioEstadoConductorDTO {

    @NotNull
    private EstadoConductor estadoConductor; // PO / EA / RO -- el nuevo estado que se quiere asignar
}
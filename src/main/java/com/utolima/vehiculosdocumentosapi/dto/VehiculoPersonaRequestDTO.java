package com.utolima.vehiculosdocumentosapi.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VehiculoPersonaRequestDTO {

    @NotNull
    private Long idPersona; // referencia a una Persona ya existente, DEBE ser tipo CONDUCTOR (se valida en el servicio)

    @NotNull
    private LocalDate fechaAsociacion;

    // no incluye estadoConductor: igual que con los documentos, el servicio decide el estado inicial,
    // no se lo dejamos elegir a quien hace la peticion
}
package com.utolima.vehiculosdocumentosapi.dto;

import com.utolima.vehiculosdocumentosapi.model.TipoIdentificacion;
import com.utolima.vehiculosdocumentosapi.model.TipoPersona;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PersonaRequestDTO {

    @NotBlank
    private String identificacion;

    @NotNull
    private TipoIdentificacion tipoIdentificacion;

    @NotBlank
    private String nombres;

    @NotBlank
    private String apellidos;

    @NotBlank
    @Email
    private String correoElectronico;

    @NotNull
    private TipoPersona tipoPersona;
}
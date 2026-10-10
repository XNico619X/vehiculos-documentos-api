package com.utolima.vehiculosdocumentosapi.dto;

import java.time.LocalDate;

import com.utolima.vehiculosdocumentosapi.model.enums.TipoIdentificacion;
import com.utolima.vehiculosdocumentosapi.model.enums.TipoPersona;

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
    
 // Solo aplican si tipoPersona = C (CONDUCTOR); ambos son opcionales
    private String licenciaConduccionBase64; // el PDF de la licencia en Base64
    private LocalDate fechaVigenciaLicencia;
}
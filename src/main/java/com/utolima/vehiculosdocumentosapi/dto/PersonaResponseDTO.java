package com.utolima.vehiculosdocumentosapi.dto;

import com.utolima.vehiculosdocumentosapi.model.enums.TipoIdentificacion;
import com.utolima.vehiculosdocumentosapi.model.enums.TipoPersona;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PersonaResponseDTO {

    private Long idPersona;
    private String identificacion;
    private TipoIdentificacion tipoIdentificacion;
    private String nombres;
    private String apellidos;
    private String correoElectronico;
    private TipoPersona tipoPersona;

    // null si tipoPersona es CONDUCTOR — solo trae datos si es ADMINISTRATIVO
    private UsuarioResponseDTO usuario;
}
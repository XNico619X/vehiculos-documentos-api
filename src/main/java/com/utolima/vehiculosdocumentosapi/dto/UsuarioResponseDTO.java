package com.utolima.vehiculosdocumentosapi.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioResponseDTO {

    private String login;
    private String password; // solo viaja en la respuesta al momento de crearse
    private String apikey;
}
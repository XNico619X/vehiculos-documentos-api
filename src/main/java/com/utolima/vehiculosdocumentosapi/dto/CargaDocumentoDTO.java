package com.utolima.vehiculosdocumentosapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CargaDocumentoDTO {
    
    @NotNull(message = "El id del documento es obligatorio")
    private Long idDocumento;

    @NotBlank(message = "El archivo en Base64 no puede estar vacío")
    private String documentoBase64;
}
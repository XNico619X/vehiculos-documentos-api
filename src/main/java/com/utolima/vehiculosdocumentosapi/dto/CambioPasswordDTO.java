package com.utolima.vehiculosdocumentosapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CambioPasswordDTO {
    
    @NotBlank(message = "El password actual no puede estar vacío")
    private String passwordActual;

    @NotBlank(message = "El password nuevo no puede estar vacío")
    @Size(min = 8, message = "El password nuevo debe tener al menos 8 caracteres")
    private String passwordNuevo;
}
package com.utolima.vehiculosdocumentosapi.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentoAsociadoRequestDTO {

    @NotNull
    private Long idDocumento; // referencia a un Documento ya existente en la tabla parametrica

    @NotNull
    private LocalDate fechaExpedicion;

    @NotNull
    private LocalDate fechaVencimiento;

    // no incluye estadoDocumento: eso lo decide el servicio (siempre inicia En Verificacion),
    // no se lo dejamos elegir a quien hace la peticion
}
package com.utolima.vehiculosdocumentosapi.dto;

import java.time.LocalDate;

import com.utolima.vehiculosdocumentosapi.model.enums.EstadoDocumento;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class DocumentoAsociadoResponseDTO {
    private Long idDocumento;
    private String codigoDocumento;
    private String nombreDocumento;
    private LocalDate fechaExpedicion;
    private LocalDate fechaVencimiento;
    private EstadoDocumento estadoDocumento;
    // OJO: no incluye ningun campo "vehiculo" -- eso es justo lo que rompe el ciclo
}

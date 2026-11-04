package com.utolima.vehiculosdocumentosapi.dto;

import java.util.Map;

public record CargueResultadoDTO(
        Long idCargue,
        int totalFilas,
        Map<String, Long> resumen
) {
}


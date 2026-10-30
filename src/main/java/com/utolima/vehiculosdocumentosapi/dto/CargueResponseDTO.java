package com.utolima.vehiculosdocumentosapi.dto;

import java.util.List;
import java.util.Map;

// Respuesta completa de la consulta de un cargue: el resumen por estado y todas sus filas.
public record CargueResponseDTO(
        Long idCargue,                          // yyyyMMddHHmm del momento del cargue
        int total,                              // cuántas filas tiene el cargue
        Map<String, Long> resumen,              // cuántas hay en cada estado (siempre los cuatro, con 0 si no hay)
        List<TrayectoTmpResponseDTO> registros) { // las filas, ordenadas por idTrayectoTmp
}
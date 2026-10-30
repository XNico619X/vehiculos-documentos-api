package com.utolima.vehiculosdocumentosapi.service;

import com.utolima.vehiculosdocumentosapi.dto.CargueResponseDTO;

public interface CargueConsultaService {

    /** Devuelve las filas de un cargue con el resumen por estado. Lanza 404 si el cargue no existe. */
    CargueResponseDTO consultarCargue(Long idCargue);
}
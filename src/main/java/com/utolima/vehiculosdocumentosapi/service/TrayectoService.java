package com.utolima.vehiculosdocumentosapi.service;

import com.utolima.vehiculosdocumentosapi.dto.TrayectoRequestDTO;
import com.utolima.vehiculosdocumentosapi.dto.TrayectoResponseDTO;

import java.util.List;

public interface TrayectoService {
    
    /**
     * Valida y registra la lista de paradas que componen una ruta (entre 2 y 7 paradas).
     * Requiere que el vehículo tenga documentos en estado Habilitado y que el conductor esté en estado PO.
     */

    void crearRuta(List<TrayectoRequestDTO> paradas);
        /** Consulta 1: paradas de una ruta ordenadas por ordenParada. Lanza 404 si el código no existe. */
    List<TrayectoResponseDTO> consultarRutaPorCodigo(String codigoRuta);
}
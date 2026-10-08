package com.utolima.vehiculosdocumentosapi.service;

import com.utolima.vehiculosdocumentosapi.dto.TrayectoRequestDTO;
import java.util.List;

public interface TrayectoService {
    
    /**
     * Valida y registra la lista de paradas que componen una ruta (entre 2 y 7 paradas).
     * Requiere que el vehículo tenga documentos en estado Habilitado y que el conductor esté en estado PO.
     */
    void crearRuta(List<TrayectoRequestDTO> paradas);
}
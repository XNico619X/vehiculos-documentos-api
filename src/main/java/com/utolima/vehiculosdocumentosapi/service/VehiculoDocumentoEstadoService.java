package com.utolima.vehiculosdocumentosapi.service;

import com.utolima.vehiculosdocumentosapi.dto.CambioEstadoDocumentoDTO;
import com.utolima.vehiculosdocumentosapi.dto.DocumentoVehiculoEstadoResponseDTO;

public interface VehiculoDocumentoEstadoService {

    /**
     * Cambia el estado (y opcionalmente la fecha de vencimiento) de un documento de un vehiculo.
     * Lanza 404 si el vehiculo no tiene ese documento y 400 si se intenta habilitar un documento vencido.
     */
    DocumentoVehiculoEstadoResponseDTO cambiarEstado(Long idVehiculo, Long idDocumento, CambioEstadoDocumentoDTO dto);
}
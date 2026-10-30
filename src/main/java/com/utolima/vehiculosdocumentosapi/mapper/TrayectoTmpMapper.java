package com.utolima.vehiculosdocumentosapi.mapper;

import java.util.List;

import com.utolima.vehiculosdocumentosapi.dto.TrayectoTmpResponseDTO;
import com.utolima.vehiculosdocumentosapi.model.TrayectoTmp;

// Convierte la entidad TrayectoTmp en DTO. Mismo estilo que TrayectoMapper: métodos estáticos.
public final class TrayectoTmpMapper {

    private TrayectoTmpMapper() { } // clase de utilidad: nadie debe crear instancias

    public static TrayectoTmpResponseDTO toResponseDTO(TrayectoTmp t) {
        return new TrayectoTmpResponseDTO(
                t.getIdTrayectoTmp(),
                t.getIdPersona(),
                t.getIdVehiculo(),
                t.getCodigoRuta(),
                t.getUbicacion(),
                t.getOrdenParada(),
                t.getLatitud(),
                t.getLongitud(),
                t.getLoginUsuario(),
                t.getEstado(),
                t.getObservacion());
    }

    // Convierte la lista completa conservando el orden que traía de la base de datos
    public static List<TrayectoTmpResponseDTO> toResponseDTOList(List<TrayectoTmp> filas) {
        return filas.stream().map(TrayectoTmpMapper::toResponseDTO).toList();
    }
}
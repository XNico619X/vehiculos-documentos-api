package com.utolima.vehiculosdocumentosapi.mapper;

import java.util.List;

import com.utolima.vehiculosdocumentosapi.dto.ConductorResumenDTO;
import com.utolima.vehiculosdocumentosapi.dto.TrayectoResponseDTO;
import com.utolima.vehiculosdocumentosapi.dto.VehiculoResumenDTO;
import com.utolima.vehiculosdocumentosapi.model.Persona;
import com.utolima.vehiculosdocumentosapi.model.Trayecto;
import com.utolima.vehiculosdocumentosapi.model.Vehiculo;

// Convierte la entidad Trayecto en DTO. Mismo estilo que PersonaMapper: métodos estáticos.
public final class TrayectoMapper {

    private TrayectoMapper() { } // clase de utilidad: nadie debe crear instancias

    public static TrayectoResponseDTO toResponseDTO(Trayecto t) {
        Persona c = t.getConductor(); // relación LAZY: aquí Hibernate la carga (la sesión debe seguir abierta)
        Vehiculo v = t.getVehiculo();

        return new TrayectoResponseDTO(
                t.getIdTrayecto(),
                t.getOrdenParada(),
                t.getUbicacion(),
                t.getLatitud(),
                t.getLongitud(),
                new ConductorResumenDTO(c.getIdPersona(), c.getIdentificacion(), c.getNombres(), c.getApellidos()),
                new VehiculoResumenDTO(v.getId(), v.getPlaca()),
                t.getLoginUsuario());
    }

    // Convierte una lista completa, conservando el orden que traía de la base de datos
    public static List<TrayectoResponseDTO> toResponseDTOList(List<Trayecto> trayectos) {
        return trayectos.stream().map(TrayectoMapper::toResponseDTO).toList();
    }
}

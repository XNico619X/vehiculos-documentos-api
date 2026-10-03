package com.utolima.vehiculosdocumentosapi.service;

import java.util.List;

import com.utolima.vehiculosdocumentosapi.dto.CambioEstadoConductorDTO;
import com.utolima.vehiculosdocumentosapi.dto.VehiculoPersonaRequestDTO;
import com.utolima.vehiculosdocumentosapi.model.VehiculoPersona;

public interface VehiculoPersonaService {

    // asocia un conductor a un vehiculo (arranca en estado EA, Espera de Aprobacion)
    VehiculoPersona asociarConductor(Long idVehiculo, VehiculoPersonaRequestDTO dto);

    // cambia el estado (PO/EA/RO) de una asociacion vehiculo-conductor ya existente
    VehiculoPersona cambiarEstado(Long idVehiculo, Long idPersona, CambioEstadoConductorDTO dto);

    // todos los conductores asociados a un vehiculo especifico
    List<VehiculoPersona> listarConductoresDeVehiculo(Long idVehiculo);

    // todos los vehiculos que puede operar (o esta en tramite de operar) un conductor especifico
    List<VehiculoPersona> listarVehiculosDeConductor(Long idPersona);
}
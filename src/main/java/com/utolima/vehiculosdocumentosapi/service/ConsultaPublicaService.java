package com.utolima.vehiculosdocumentosapi.service;

import java.time.LocalDate;
import java.util.List;

import com.utolima.vehiculosdocumentosapi.dto.ConteoPersonaPorTipoDTO;
import com.utolima.vehiculosdocumentosapi.dto.VehiculoDetalleCompletoDTO;
import com.utolima.vehiculosdocumentosapi.model.Persona;
import com.utolima.vehiculosdocumentosapi.model.Vehiculo;

public interface ConsultaPublicaService {

    List<Vehiculo> vehiculosConDocumentosVencidos();

    List<Vehiculo> vehiculosConDocumentosPorVencer(int dias);

    List<Persona> conductoresQuePuedenOperar();

    VehiculoDetalleCompletoDTO vehiculoCompletoPorPlaca(String placa);

    List<ConteoPersonaPorTipoDTO> totalPersonasPorTipo();
}
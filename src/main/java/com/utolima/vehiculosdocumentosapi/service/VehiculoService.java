package com.utolima.vehiculosdocumentosapi.service;

import java.util.List;

import com.utolima.vehiculosdocumentosapi.dto.DocumentoAsociadoRequestDTO;
import com.utolima.vehiculosdocumentosapi.dto.VehiculoRequestDTO;
import com.utolima.vehiculosdocumentosapi.model.Vehiculo;
import com.utolima.vehiculosdocumentosapi.model.VehiculoDocumento;
import com.utolima.vehiculosdocumentosapi.model.enums.EstadoDocumento;
import com.utolima.vehiculosdocumentosapi.model.enums.TipoVehiculo;

// La interfaz define QUE operaciones existen, sin decir COMO se implementan
public interface VehiculoService {

    Vehiculo crear(VehiculoRequestDTO dto);

    Vehiculo obtenerPorId(Long id);

    List<Vehiculo> listarTodos();

    Vehiculo actualizar(Long id, VehiculoRequestDTO dto);

    void eliminar(Long id);

    Vehiculo buscarPorPlaca(String placa);

    List<Vehiculo> buscarPorTipoVehiculo(TipoVehiculo tipoVehiculo);

    List<Vehiculo> buscarPorDocumento(Long idDocumento);

    List<Vehiculo> buscarPorEstadoDocumento(EstadoDocumento estado);

    VehiculoDocumento agregarDocumento(Long idVehiculo, DocumentoAsociadoRequestDTO dto);
}
package com.utolima.vehiculosdocumentosapi.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.utolima.vehiculosdocumentosapi.model.VehiculoDocumento;
import com.utolima.vehiculosdocumentosapi.model.VehiculoDocumentoId;

// La llave primaria de VehiculoDocumento es compuesta (VehiculoDocumentoId), por eso va como segundo parametro
public interface VehiculoDocumentoRepository extends JpaRepository<VehiculoDocumento, VehiculoDocumentoId> {

    // Todos los registros de la tabla puente para un vehiculo dado
    // "Vehiculo_Id" con guion bajo: significa "el campo 'id' dentro de la relacion 'vehiculo'"
    List<VehiculoDocumento> findByVehiculo_Id(Long idVehiculo);

    // Todos los registros de la tabla puente para un documento dado
    List<VehiculoDocumento> findByDocumento_Id(Long idDocumento);
}
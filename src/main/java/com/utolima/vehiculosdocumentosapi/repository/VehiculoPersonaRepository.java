package com.utolima.vehiculosdocumentosapi.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.utolima.vehiculosdocumentosapi.model.Persona;
import com.utolima.vehiculosdocumentosapi.model.VehiculoPersona;
import com.utolima.vehiculosdocumentosapi.model.VehiculoPersonaId;
import com.utolima.vehiculosdocumentosapi.model.enums.EstadoConductor;

@Repository
public interface VehiculoPersonaRepository extends JpaRepository<VehiculoPersona, VehiculoPersonaId> { // el ID vuelve a ser VehiculoPersonaId

    List<VehiculoPersona> findByVehiculo_Id(Long idVehiculo);          // Vehiculo sí tiene un campo "id"
    List<VehiculoPersona> findByPersona_IdPersona(Long idPersona);     // Persona tiene "idPersona"

    // se declara UNA sola vez, con su @Query
    @Query("SELECT DISTINCT vp.persona FROM VehiculoPersona vp WHERE vp.estadoConductor = :estado")
    List<Persona> findPersonasPorEstadoConductor(@Param("estado") EstadoConductor estado);

    // findByVehiculoAndPersona se elimina: no hace falta, basta findById(new VehiculoPersonaId(...))
}
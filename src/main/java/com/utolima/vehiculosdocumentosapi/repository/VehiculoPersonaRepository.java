package com.utolima.vehiculosdocumentosapi.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.utolima.vehiculosdocumentosapi.model.VehiculoPersona;
import com.utolima.vehiculosdocumentosapi.model.VehiculoPersonaId;

import com.utolima.vehiculosdocumentosapi.model.Persona;
import com.utolima.vehiculosdocumentosapi.model.enums.EstadoConductor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VehiculoPersonaRepository extends JpaRepository<VehiculoPersona, VehiculoPersonaId> {

    // Vehiculo si tiene un campo llamado "id" (de la Entrega 1), asi que esta funciona tal cual
	List<VehiculoPersona> findByVehiculo_Id(Long idVehiculo);

    // Persona tiene su PK llamada "idPersona", NO "id" por eso el nombre del metodo cambia
    List<VehiculoPersona> findByPersona_IdPersona(Long idPersona);

 // "todos los conductores que puedan operar" = personas con AL MENOS una asociacion en estado PO
    @Query("SELECT DISTINCT vp.persona FROM VehiculoPersona vp WHERE vp.estadoConductor = :estado")
    List<Persona> findPersonasPorEstadoConductor(@Param("estado") EstadoConductor estado);
}
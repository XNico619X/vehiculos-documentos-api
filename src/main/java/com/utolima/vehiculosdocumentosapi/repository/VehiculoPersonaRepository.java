package com.utolima.vehiculosdocumentosapi.repository;

import com.utolima.vehiculosdocumentosapi.model.Persona;
import com.utolima.vehiculosdocumentosapi.model.VehiculoPersona;
import com.utolima.vehiculosdocumentosapi.model.enums.EstadoConductor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehiculoPersonaRepository extends JpaRepository<VehiculoPersona, Object> {

    @Query("SELECT vp FROM VehiculoPersona vp WHERE vp.vehiculo.idVehiculo = :idVehiculo AND vp.persona.idPersona = :idPersona")
    Optional<VehiculoPersona> findByVehiculoAndPersona(@Param("idVehiculo") Long idVehiculo, @Param("idPersona") Long idPersona);

    List<Persona> findPersonasPorEstadoConductor(EstadoConductor puedeOperar);
    List<VehiculoPersona> findByPersona_IdPersona(Long idPersona);
    List<VehiculoPersona> findByVehiculo_Id(Long idVehiculo);
}
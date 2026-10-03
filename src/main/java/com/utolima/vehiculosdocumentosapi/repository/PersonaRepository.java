package com.utolima.vehiculosdocumentosapi.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.utolima.vehiculosdocumentosapi.model.Persona;

import com.utolima.vehiculosdocumentosapi.dto.ConteoPersonaPorTipoDTO;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PersonaRepository extends JpaRepository<Persona, Long> {

    Optional<Persona> findByIdentificacion(String identificacion);

 // "constructor expression" de JPQL: en vez de devolver Persona, construye el DTO directamente en la consulta
    @Query("SELECT new com.utolima.vehiculosdocumentosapi.dto.ConteoPersonaPorTipoDTO(p.tipoPersona, COUNT(p)) " +
           "FROM Persona p GROUP BY p.tipoPersona")
    List<ConteoPersonaPorTipoDTO> contarPersonasPorTipo();
}
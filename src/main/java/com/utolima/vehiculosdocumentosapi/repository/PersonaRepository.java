package com.utolima.vehiculosdocumentosapi.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.utolima.vehiculosdocumentosapi.model.Persona;

public interface PersonaRepository extends JpaRepository<Persona, Long> {

    Optional<Persona> findByIdentificacion(String identificacion);
}
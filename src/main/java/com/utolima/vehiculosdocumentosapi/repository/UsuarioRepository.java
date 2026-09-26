package com.utolima.vehiculosdocumentosapi.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.utolima.vehiculosdocumentosapi.model.Usuario;
import com.utolima.vehiculosdocumentosapi.model.UsuarioId;

public interface UsuarioRepository extends JpaRepository<Usuario, UsuarioId> {

    Optional<Usuario> findByIdLogin(String login);

    Optional<Usuario> findByIdIdPersona(Long idPersona);
}
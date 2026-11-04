package com.utolima.vehiculosdocumentosapi.service;

import com.utolima.vehiculosdocumentosapi.dto.PersonaRequestDTO;
import com.utolima.vehiculosdocumentosapi.dto.PersonaResponseDTO;

public interface ConfiguracionInicialService {

    /** true si todavia no existe ningun usuario (instalacion nueva). */
    boolean requiereConfiguracionInicial();

    /**
     * Crea el PRIMER administrador (con su usuario). Solo funciona mientras no exista ningun usuario;
     * despues responde 400. Los demas administradores los crea un administrador con sesion.
     */
    PersonaResponseDTO crearPrimerAdministrador(PersonaRequestDTO dto);
}
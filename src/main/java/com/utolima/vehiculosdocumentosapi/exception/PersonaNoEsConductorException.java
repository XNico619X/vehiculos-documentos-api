package com.utolima.vehiculosdocumentosapi.exception;

public class PersonaNoEsConductorException extends RuntimeException {

    private static final long serialVersionUID = 1L; // fija la version, evita el warning que ya conoces

    public PersonaNoEsConductorException(String mensaje) {
        super(mensaje);
    }
}
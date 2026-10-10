package com.utolima.vehiculosdocumentosapi.exception;

// Para reglas de negocio que el cliente puede corregir (ruta mal armada, conductor sin permiso, etc.). Se traduce a 400.
public class ReglaNegocioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
package com.utolima.vehiculosdocumentosapi.exception;

public class VehiculoSinDocumentoException extends RuntimeException {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L; // fija la versión, evita el warning y futuros problemas de deserialización

	public VehiculoSinDocumentoException(String mensaje) {
        super(mensaje);
    }
}
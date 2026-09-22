package com.utolima.vehiculosdocumentosapi.exception;

//extiende RuntimeException (no checked exception) para no obligar a un try-catch en cada llamada
public class RecursoNoEncontradoException extends RuntimeException {
 /**
	 * 
	 */
	private static final long serialVersionUID = 1L; // fija la versión, evita el warning y futuros problemas de deserialización

 public RecursoNoEncontradoException(String mensaje) {
     super(mensaje); // pasa el mensaje al constructor de la clase padre (RuntimeException)
 }
}
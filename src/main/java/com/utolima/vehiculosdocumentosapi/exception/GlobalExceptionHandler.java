package com.utolima.vehiculosdocumentosapi.exception;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.utolima.vehiculosdocumentosapi.dto.ErrorResponseDTO;

@RestControllerAdvice // se aplica a TODOS los @RestController de la aplicacion, sin tener que repetir nada en cada uno
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class) // captura especificamente este tipo de excepcion
    public ResponseEntity<ErrorResponseDTO> manejarNoEncontrado(RecursoNoEncontradoException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO(LocalDateTime.now(), 404, ex.getMessage(), null);
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND); // responde 404
    }

    @ExceptionHandler(VehiculoSinDocumentoException.class)
    public ResponseEntity<ErrorResponseDTO> manejarSinDocumento(VehiculoSinDocumentoException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO(LocalDateTime.now(), 400, ex.getMessage(), null);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST); // responde 400
    }

    @ExceptionHandler(IllegalArgumentException.class) // captura, por ejemplo, cuando fromCodigo() no encuentra el enum
    public ResponseEntity<ErrorResponseDTO> manejarArgumentoInvalido(IllegalArgumentException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO(LocalDateTime.now(), 400, ex.getMessage(), null);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class) // se dispara cuando @Valid encuentra campos invalidos en un @RequestBody
    public ResponseEntity<ErrorResponseDTO> manejarValidacion(MethodArgumentNotValidException ex) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage()) // ej. "placa: size must be between 6 and 6"
                .toList();
        ErrorResponseDTO error = new ErrorResponseDTO(LocalDateTime.now(), 400, "Error de validación", detalles);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
}
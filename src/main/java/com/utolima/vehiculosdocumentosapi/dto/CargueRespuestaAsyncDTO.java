package com.utolima.vehiculosdocumentosapi.dto;

/**
 * DTO de respuesta inmediata devuelto por el endpoint POST /api/trayectos/cargues (HTTP 202 Accepted).
 *
 * Confirma al cliente que el archivo fue validado y almacenado en la tabla temporal,
 * y que su procesamiento y validacion de reglas de negocio han sido encolados en RabbitMQ.
 *
 * @param idCargue   Identificador del cargue asignado para consultar su estado posterior.
 * @param totalFilas Total de filas leidas y guardadas en estado CARGADO.
 * @param estado     Estado inicial del cargue ("RECIBIDO").
 * @param mensaje    Mensaje informativo para el cliente con la instruccion para consultar el avance.
 */
public record CargueRespuestaAsyncDTO(
        Long idCargue,
        int totalFilas,
        String estado,
        String mensaje
) {
}


package com.utolima.vehiculosdocumentosapi.dto;

import java.io.Serializable;

/**
 * Mensaje enviado a traves de RabbitMQ para solicitar el procesamiento en segundo plano
 * de un cargue de trayectos almacenado previamente en trayecto_tmp.
 *
 * @param idCargue   Identificador del cargue generado con yyyyMMddHHmm.
 * @param totalFilas Cantidad total de filas de datos registradas en la tabla temporal.
 * @param login      Login del usuario autenticado que inicio el cargue.
 */
public record CargueMensajeDTO(
        Long idCargue,
        int totalFilas,
        String login
) implements Serializable {
}


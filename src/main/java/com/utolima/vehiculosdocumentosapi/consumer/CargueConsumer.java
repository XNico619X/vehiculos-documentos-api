package com.utolima.vehiculosdocumentosapi.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.utolima.vehiculosdocumentosapi.config.RabbitMQConfig;
import com.utolima.vehiculosdocumentosapi.dto.CargueMensajeDTO;
import com.utolima.vehiculosdocumentosapi.service.CargueService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumidor de RabbitMQ que escucha la cola de cargues de Excel.
 *
 * Desacopla la ejecucion pesada de sp_validacion y sp_procesado del hilo HTTP
 * principal. En caso de fallas tecnicas, Spring AMQP reintenta segun la politica
 * configurada y, de persistir el error, envia el mensaje a la Dead Letter Queue (DLQ).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CargueConsumer {

    private final CargueService cargueService;

    @RabbitListener(queues = RabbitMQConfig.COLA_CARGUE)
    public void procesarCargue(CargueMensajeDTO mensaje) {
        log.info("Mensaje recibido de RabbitMQ para cargue: {}, total filas: {}, usuario: {}",
                mensaje.idCargue(), mensaje.totalFilas(), mensaje.login());

        cargueService.procesarCargueAsincrono(mensaje.idCargue());

        log.info("Procesamiento completado con exito para cargue: {}", mensaje.idCargue());
    }
 // Escucha la cola de mensajes fallidos: aqui llegan los cargues que fallaron tras todos los reintentos.
    @RabbitListener(queues = RabbitMQConfig.COLA_DLQ)
    public void cargueFallido(CargueMensajeDTO mensaje) {
        log.error("El cargue {} llego a la cola de mensajes fallidos; se marca como ERROR", mensaje.idCargue());
        cargueService.marcarCargueFallido(mensaje.idCargue(),
                "El procesamiento falló tras varios intentos y no se pudo completar");
    }
}


package com.utolima.vehiculosdocumentosapi.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion de RabbitMQ para el procesamiento asincrono de archivos Excel.
 *
 * Define la topologia de mensajeria:
 * - Exchange principal directo (cargue.direct.exchange) y cola principal (cargue.excel.cola).
 * - Dead Letter Exchange (cargue.dlx.exchange) y Dead Letter Queue (cargue.excel.dlq) para
 *   almacenar mensajes que hayan superado el numero maximo de reintentos fallidos.
 * - MessageConverter para enviar los mensajes DTO como JSON.
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_CARGUE = "cargue.direct.exchange";
    public static final String COLA_CARGUE = "cargue.excel.cola";
    public static final String ROUTING_KEY_CARGUE = "cargue.excel.key";

    public static final String EXCHANGE_DLX = "cargue.dlx.exchange";
    public static final String COLA_DLQ = "cargue.excel.dlq";
    public static final String ROUTING_KEY_DLQ = "cargue.excel.dlq.key";

    /**
     * Cola principal de cargues con configuracion de Dead Letter Queue (DLQ).
     * Si un mensaje falla tras agotar los reintentos definidos en application.properties,
     * se enruta automaticamente al exchange DLX y cae en la cola DLQ para auditoria o analisis.
     */
    @Bean
    public Queue colaCargue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", EXCHANGE_DLX);
        args.put("x-dead-letter-routing-key", ROUTING_KEY_DLQ);
        return QueueBuilder.durable(COLA_CARGUE)
                .withArguments(args)
                .build();
    }

    @Bean
    public DirectExchange exchangeCargue() {
        return new DirectExchange(EXCHANGE_CARGUE);
    }

    @Bean
    public Binding bindingCargue(Queue colaCargue, DirectExchange exchangeCargue) {
        return BindingBuilder.bind(colaCargue)
                .to(exchangeCargue)
                .with(ROUTING_KEY_CARGUE);
    }

    /**
     * Cola Dead Letter Queue (DLQ) para mensajes fallidos.
     */
    @Bean
    public Queue colaDlq() {
        return QueueBuilder.durable(COLA_DLQ).build();
    }

    @Bean
    public DirectExchange exchangeDlx() {
        return new DirectExchange(EXCHANGE_DLX);
    }

    @Bean
    public Binding bindingDlq(Queue colaDlq, DirectExchange exchangeDlx) {
        return BindingBuilder.bind(colaDlq)
                .to(exchangeDlx)
                .with(ROUTING_KEY_DLQ);
    }

    /**
     * Conversor JSON para serializar y deserializar automaticamente los mensajes DTO.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}


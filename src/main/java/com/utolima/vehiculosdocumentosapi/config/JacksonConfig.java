package com.utolima.vehiculosdocumentosapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class JacksonConfig {

    @Bean // ahora SI existe un bean de tipo ObjectMapper en el contexto -- esto es lo que faltaba
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule()); // le ensena a Jackson a leer/escribir LocalDate, LocalDateTime, etc.
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // sin esto, las fechas saldrian como numeros raros (epoch millis) en vez de texto legible
        return mapper;
    }
}
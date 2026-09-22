package com.utolima.vehiculosdocumentosapi.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target(ElementType.TYPE) // se aplica sobre una CLASE completa (TYPE), no sobre un campo (FIELD) como @NotNull
@Retention(RetentionPolicy.RUNTIME) // debe existir en tiempo de ejecucion, no solo en el codigo fuente
@Constraint(validatedBy = PlacaValidaValidator.class) // conecta esta anotacion con la clase que tiene la logica real
public @interface PlacaValida {
    String message() default "El formato de la placa no coincide con el tipo de vehículo"; // mensaje por defecto
    Class<?>[] groups() default {};   // exigido por la especificacion de Bean Validation, casi nunca se usa
    Class<? extends Payload>[] payload() default {}; // igual, exigido por la especificacion, casi nunca se usa
}
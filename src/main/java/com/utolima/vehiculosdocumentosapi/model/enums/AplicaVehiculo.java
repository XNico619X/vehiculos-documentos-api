package com.utolima.vehiculosdocumentosapi.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AplicaVehiculo {
    AUTOMOVIL("A"),
    MOTOCICLETA("M"),
    AMBOS("AM");

    private final String codigo;

    AplicaVehiculo(String codigo) {
        this.codigo = codigo;
    }
    
    @JsonValue // Jackson usa esto para SERIALIZAR: al devolver JSON, escribe "Pu" en vez de "PUBLICO"
    public String getCodigo() {
        return codigo;
    }

    @JsonCreator // Jackson usa esto para DESERIALIZAR: al recibir "Pu" en el JSON, construye TipoServicio.PUBLICO
    public static AplicaVehiculo fromCodigo(String codigo) {
        for (AplicaVehiculo av : values()) {
            if (av.codigo.equals(codigo)) {
                return av;
            }
        }
        throw new IllegalArgumentException("Código de aplicación de vehículo no válido: " + codigo);
    }
}
package com.utolima.vehiculosdocumentosapi.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Obligatoriedad {
    REQUERIDO_AUTOMOVIL("RA"),
    REQUERIDO_MOTOCICLETA("RM"),
    REQUERIDO_AMBOS("RR");

    private final String codigo;

    Obligatoriedad(String codigo) {
        this.codigo = codigo;
    }
    @JsonValue // Jackson usa esto para SERIALIZAR: al devolver JSON, escribe "Pu" en vez de "PUBLICO"
    public String getCodigo() {
        return codigo;
    }

    @JsonCreator // Jackson usa esto para DESERIALIZAR: al recibir "Pu" en el JSON, construye TipoServicio.PUBLICO
    public static Obligatoriedad fromCodigo(String codigo) {
        for (Obligatoriedad o : values()) {
            if (o.codigo.equals(codigo)) {
                return o;
            }
        }
        throw new IllegalArgumentException("Código de obligatoriedad no válido: " + codigo);
    }
}
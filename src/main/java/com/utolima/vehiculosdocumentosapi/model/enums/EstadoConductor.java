package com.utolima.vehiculosdocumentosapi.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EstadoConductor {
    PUEDE_OPERAR("PO"),
    ESPERA_APROBACION("EA"),
    RESTRINGIDO("RO");

    private final String codigo;

    EstadoConductor(String codigo) {
        this.codigo = codigo;
    }

    @JsonValue
    public String getCodigo() {
        return codigo;
    }

    @JsonCreator
    public static EstadoConductor fromCodigo(String codigo) {
        for (EstadoConductor e : values()) {
            if (e.codigo.equals(codigo)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Código de estado de conductor no válido: " + codigo);
    }
}
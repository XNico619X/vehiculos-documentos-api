package com.utolima.vehiculosdocumentosapi.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TipoPersona {
    CONDUCTOR("C"),
    ADMINISTRATIVO("A");

    private final String codigo;

    TipoPersona(String codigo) {
        this.codigo = codigo;
    }

    @JsonValue
    public String getCodigo() {
        return codigo;
    }

    public static TipoPersona fromCodigo(String codigo) {
        for (TipoPersona t : values()) {
            if (t.codigo.equals(codigo)) return t;
        }
        throw new IllegalArgumentException("Código de tipo de persona inválido: " + codigo);
    }
}
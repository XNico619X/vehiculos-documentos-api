package com.utolima.vehiculosdocumentosapi.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TipoCombustible {
    GASOLINA("Gasolina"), // ojo: el CHECK de la BD exige mayúscula solo en la primera letra
    GAS("Gas"),
    DIESEL("Disel");      // se guarda como "Disel" para coincidir exacto con el CHECK constraint

    private final String codigo;

    TipoCombustible(String codigo) {
        this.codigo = codigo;
    }
    @JsonValue // Jackson usa esto para SERIALIZAR: al devolver JSON, escribe "Pu" en vez de "PUBLICO"
    public String getCodigo() {
        return codigo;
    }
    @JsonCreator // Jackson usa esto para DESERIALIZAR: al recibir "Pu" en el JSON, construye TipoServicio.PUBLICO
    public static TipoCombustible fromCodigo(String codigo) {
        for (TipoCombustible tc : values()) {
            if (tc.codigo.equals(codigo)) {
                return tc;
            }
        }
        throw new IllegalArgumentException("Código de tipo de combustible no válido: " + codigo);
    }
}
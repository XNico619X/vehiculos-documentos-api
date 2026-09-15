package com.utolima.vehiculosdocumentosapi.model.enums;

public enum TipoCombustible {
    GASOLINA("Gasolina"), // ojo: el CHECK de la BD exige mayúscula solo en la primera letra
    GAS("Gas"),
    DIESEL("Disel");      // se guarda como "Disel" para coincidir exacto con el CHECK constraint

    private final String codigo;

    TipoCombustible(String codigo) {
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }

    public static TipoCombustible fromCodigo(String codigo) {
        for (TipoCombustible tc : values()) {
            if (tc.codigo.equals(codigo)) {
                return tc;
            }
        }
        throw new IllegalArgumentException("Código de tipo de combustible no válido: " + codigo);
    }
}
package com.utolima.vehiculosdocumentosapi.model.enums;

public enum Obligatoriedad {
    REQUERIDO_AUTOMOVIL("RA"),
    REQUERIDO_MOTOCICLETA("RM"),
    REQUERIDO_AMBOS("RR");

    private final String codigo;

    Obligatoriedad(String codigo) {
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }

    public static Obligatoriedad fromCodigo(String codigo) {
        for (Obligatoriedad o : values()) {
            if (o.codigo.equals(codigo)) {
                return o;
            }
        }
        throw new IllegalArgumentException("Código de obligatoriedad no válido: " + codigo);
    }
}
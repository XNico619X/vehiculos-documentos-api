package com.utolima.vehiculosdocumentosapi.model.enums;

public enum AplicaVehiculo {
    AUTOMOVIL("A"),
    MOTOCICLETA("M"),
    AMBOS("AM");

    private final String codigo;

    AplicaVehiculo(String codigo) {
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }

    public static AplicaVehiculo fromCodigo(String codigo) {
        for (AplicaVehiculo av : values()) {
            if (av.codigo.equals(codigo)) {
                return av;
            }
        }
        throw new IllegalArgumentException("Código de aplicación de vehículo no válido: " + codigo);
    }
}
package com.utolima.vehiculosdocumentosapi.model.enums;

public enum TipoVehiculo {
    AUTOMOVIL("AUTOMOVIL"),   // constante Java -> valor exacto que exige el CHECK en la tabla vehiculos
    MOTOCICLETA("MOTOCICLETA");

    private final String codigo; // guarda el texto real que se escribe/lee en la columna tipo_vehiculo

    TipoVehiculo(String codigo) { // constructor: se ejecuta al crear cada constante de arriba
        this.codigo = codigo;
    }

    public String getCodigo() { // getter: usado por el converter para saber qué mandar a la BD
        return codigo;
    }

    public static TipoVehiculo fromCodigo(String codigo) { // camino inverso: BD -> Java
        for (TipoVehiculo tv : values()) { // values() recorre todas las constantes del enum
            if (tv.codigo.equals(codigo)) {
                return tv;
            }
        }
        throw new IllegalArgumentException("Código de tipo de vehículo no válido: " + codigo);
    }
}
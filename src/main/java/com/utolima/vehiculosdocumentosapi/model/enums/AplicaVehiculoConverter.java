package com.utolima.vehiculosdocumentosapi.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AplicaVehiculoConverter implements AttributeConverter<AplicaVehiculo, String> {

    @Override
    public String convertToDatabaseColumn(AplicaVehiculo aplicaVehiculo) {
        return aplicaVehiculo == null ? null : aplicaVehiculo.getCodigo();
    }

    @Override
    public AplicaVehiculo convertToEntityAttribute(String codigo) {
        return codigo == null ? null : AplicaVehiculo.fromCodigo(codigo);
    }
}
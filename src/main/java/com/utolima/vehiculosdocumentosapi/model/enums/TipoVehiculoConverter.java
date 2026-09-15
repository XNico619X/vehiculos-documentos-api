package com.utolima.vehiculosdocumentosapi.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true) // se aplica solo a cualquier campo de tipo TipoVehiculo en cualquier entidad
public class TipoVehiculoConverter implements AttributeConverter<TipoVehiculo, String> {

    @Override
    public String convertToDatabaseColumn(TipoVehiculo tipoVehiculo) { // Java -> BD (al guardar)
        return tipoVehiculo == null ? null : tipoVehiculo.getCodigo();
    }

    @Override
    public TipoVehiculo convertToEntityAttribute(String codigo) { // BD -> Java (al leer)
        return codigo == null ? null : TipoVehiculo.fromCodigo(codigo);
    }
}
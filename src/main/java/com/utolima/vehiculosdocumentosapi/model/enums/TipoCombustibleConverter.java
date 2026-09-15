package com.utolima.vehiculosdocumentosapi.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TipoCombustibleConverter implements AttributeConverter<TipoCombustible, String> {

    @Override
    public String convertToDatabaseColumn(TipoCombustible tipoCombustible) {
        return tipoCombustible == null ? null : tipoCombustible.getCodigo();
    }

    @Override
    public TipoCombustible convertToEntityAttribute(String codigo) {
        return codigo == null ? null : TipoCombustible.fromCodigo(codigo);
    }
}
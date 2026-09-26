package com.utolima.vehiculosdocumentosapi.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TipoPersonaConverter implements AttributeConverter<TipoPersona, String> {

    @Override
    public String convertToDatabaseColumn(TipoPersona tipoPersona) {
        return tipoPersona == null ? null : tipoPersona.getCodigo();
    }

    @Override
    public TipoPersona convertToEntityAttribute(String codigo) {
        return codigo == null ? null : TipoPersona.fromCodigo(codigo);
    }
}
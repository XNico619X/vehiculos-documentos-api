package com.utolima.vehiculosdocumentosapi.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ObligatoriedadConverter implements AttributeConverter<Obligatoriedad, String> {

    @Override
    public String convertToDatabaseColumn(Obligatoriedad obligatoriedad) {
        return obligatoriedad == null ? null : obligatoriedad.getCodigo();
    }

    @Override
    public Obligatoriedad convertToEntityAttribute(String codigo) {
        return codigo == null ? null : Obligatoriedad.fromCodigo(codigo);
    }
}
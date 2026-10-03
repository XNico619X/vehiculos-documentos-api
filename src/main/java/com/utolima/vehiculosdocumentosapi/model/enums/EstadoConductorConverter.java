package com.utolima.vehiculosdocumentosapi.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EstadoConductorConverter implements AttributeConverter<EstadoConductor, String> {

    @Override
    public String convertToDatabaseColumn(EstadoConductor estadoConductor) {
        return estadoConductor == null ? null : estadoConductor.getCodigo();
    }

    @Override
    public EstadoConductor convertToEntityAttribute(String codigo) {
        return codigo == null ? null : EstadoConductor.fromCodigo(codigo);
    }
}
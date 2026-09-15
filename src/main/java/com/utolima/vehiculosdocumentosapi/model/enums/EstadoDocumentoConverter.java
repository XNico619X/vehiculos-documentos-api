package com.utolima.vehiculosdocumentosapi.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EstadoDocumentoConverter implements AttributeConverter<EstadoDocumento, String> {

    @Override
    public String convertToDatabaseColumn(EstadoDocumento estadoDocumento) {
        return estadoDocumento == null ? null : estadoDocumento.getCodigo();
    }

    @Override
    public EstadoDocumento convertToEntityAttribute(String codigo) {
        return codigo == null ? null : EstadoDocumento.fromCodigo(codigo);
    }
}
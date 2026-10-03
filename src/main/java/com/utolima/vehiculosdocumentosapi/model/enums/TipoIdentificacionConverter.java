package com.utolima.vehiculosdocumentosapi.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)

public class TipoIdentificacionConverter implements AttributeConverter<TipoIdentificacion, String>{
	
	    @Override
	    public String convertToDatabaseColumn(TipoIdentificacion TipoIdentificacion) {
	    	return TipoIdentificacion == null ? null : TipoIdentificacion.getCodigo();
	    	
	    }
	    @Override
	    public TipoIdentificacion convertToEntityAttribute(String codigo) {
	        return codigo == null ? null : TipoIdentificacion.fromCodigo(codigo);
	    }

}

package com.utolima.vehiculosdocumentosapi.model.enums;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;


public enum TipoIdentificacion {
    CEDULACIUDADANIA("CC");
    
    private final String codigo;
    TipoIdentificacion(String codigo) {
        this.codigo = codigo;
    }
    
    @JsonValue // Jackson usa esto para SERIALIZAR: al devolver JSON, escribe "CC" en vez de CEDULAACIUDADANIA
    public String getCodigo() {
    	return codigo;
    }
    
    @JsonCreator // Jackson usa esto para DESERIALIZAR: al recibir "CC" en el JSON, construye TipoIdentificacion.PUBLICO
    public static TipoIdentificacion fromCodigo(String codigo){
    	for (TipoIdentificacion ti : values()) {
    		if (ti.codigo.equals(codigo)) {
    			return ti;
    		}
    	}
        throw new IllegalArgumentException("Código de tipo de identificacion no válido: " + codigo);

    }
    }
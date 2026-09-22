package com.utolima.vehiculosdocumentosapi.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;


// conjunto cerrado y fijo de valores y se le da un constructor
public enum TipoServicio {
    PUBLICO("Pu"), 
    PRIVADO("Pr");
	
	
// cada constante del enum va a guardar internamente un String.
    private final String codigo;

//constructor el parametro codigo es jjusto  'Pu' o 'Pr' y luego lo guarda en la constante PUBLICO o PRIVADO  
    TipoServicio(String codigo) {
        this.codigo = codigo;
    }
    
// Responde dependiedo al codigo que se le pida 'Pu' o 'Pr'
    @JsonValue // Jackson usa esto para SERIALIZAR: al devolver JSON, escribe "Pu" en vez de "PUBLICO"
    public String getCodigo() {
        return codigo;
    }
// 'codigo' es el nombre que yo elegi para el campo donde esperamos el valor fijo
    
// (inverso)esto hace lo contrario de la base de datos al codigo. values() devuelve un arreglo con todas sus constantes que recorre comparando el 'codigo' y tira una excepcion si no encuentra coincidiencia      
    @JsonCreator // Jackson usa esto para DESERIALIZAR: al recibir "Pu" en el JSON, construye TipoServicio.PUBLICO
    public static TipoServicio fromCodigo(String codigo) {
        for (TipoServicio ts : values()) {
            if (ts.codigo.equals(codigo)) {
                return ts;
            }
        }
        throw new IllegalArgumentException("Código de tipo de servicio no válido: " + codigo);
    }
} 
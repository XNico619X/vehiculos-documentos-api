package com.utolima.vehiculosdocumentosapi.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EstadoDocumento {
    HABILITADO("Habilitado"),
    VENCIDO("Vencido"),
    EN_VERIFICACION("En Verificacion"); // ojo: sin tilde, tal como quedó en el CHECK de la tabla

    private final String codigo;

    EstadoDocumento(String codigo) {
        this.codigo = codigo;
    }
    
    @JsonValue // Jackson usa esto para SERIALIZAR: al devolver JSON, escribe "Pu" en vez de "PUBLICO"
    public String getCodigo() {
        return codigo;
    }
    
    @JsonCreator // Jackson usa esto para DESERIALIZAR: al recibir "Pu" en el JSON, construye TipoServicio.PUBLICO
    public static EstadoDocumento fromCodigo(String codigo) {
        for (EstadoDocumento e : values()) {
            if (e.codigo.equals(codigo)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Código de estado de documento no válido: " + codigo);
    }
}
package com.utolima.vehiculosdocumentosapi.model.enums;
public enum EstadoDocumento {
    HABILITADO("Habilitado"),
    VENCIDO("Vencido"),
    EN_VERIFICACION("En Verificacion"); // ojo: sin tilde, tal como quedó en el CHECK de la tabla

    private final String codigo;

    EstadoDocumento(String codigo) {
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }

    public static EstadoDocumento fromCodigo(String codigo) {
        for (EstadoDocumento e : values()) {
            if (e.codigo.equals(codigo)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Código de estado de documento no válido: " + codigo);
    }
}
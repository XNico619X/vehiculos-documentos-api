package com.utolima.vehiculosdocumentosapi.dto;

// Respuesta de GET /api/auth/estado. Es publica, asi que dice lo MINIMO: si el sistema aun no tiene usuarios.
public record EstadoSistemaDTO(boolean requiereConfiguracionInicial) {
}
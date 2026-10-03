package com.utolima.vehiculosdocumentosapi.security;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utolima.vehiculosdocumentosapi.dto.ErrorResponseDTO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    // Spring Security llama este metodo cuando una ruta protegida recibe una peticion SIN autenticar --
    // por defecto mostraria una pagina HTML fea; aqui lo reemplazamos por nuestro formato JSON de siempre
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponseDTO error = new ErrorResponseDTO(LocalDateTime.now(), 401,
                "No autenticado: token JWT ausente, inválido o expirado", null);
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
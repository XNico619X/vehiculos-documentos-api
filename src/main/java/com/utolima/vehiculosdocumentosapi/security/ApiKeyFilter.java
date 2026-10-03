package com.utolima.vehiculosdocumentosapi.security;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utolima.vehiculosdocumentosapi.dto.ErrorResponseDTO;
import com.utolima.vehiculosdocumentosapi.model.Usuario;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ApiKeyFilter extends OncePerRequestFilter {

    // reutilizamos el ObjectMapper que YA configura Spring Boot automaticamente (sabe serializar LocalDateTime, etc.)
    // en vez de crear uno nuevo con "new ObjectMapper()", que no tendria ese soporte configurado
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // si NO hay autenticacion (el JWT no era valido o no vino), no exigimos apikey AQUI
        // dejamos pasar: si la ruta es publica, sigue igual; si no, SecurityConfig la rechaza por falta de JWT, no de apikey
        if (authentication == null || !(authentication.getPrincipal() instanceof Usuario usuario)) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKeyHeader = request.getHeader("X-API-Key");

        if (apiKeyHeader == null || !apiKeyHeader.equals(usuario.getApikey())) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ErrorResponseDTO error = new ErrorResponseDTO(LocalDateTime.now(), 401,
                    "APIKey inválida o ausente en el header X-API-Key", null);
            response.getWriter().write(objectMapper.writeValueAsString(error));
            return; // CRITICO: no llamamos filterChain.doFilter -- la peticion NUNCA llega al controlador
        }

        filterChain.doFilter(request, response);
    }
}
package com.utolima.vehiculosdocumentosapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.utolima.vehiculosdocumentosapi.security.ApiKeyFilter;
import com.utolima.vehiculosdocumentosapi.security.CustomAuthenticationEntryPoint;
import com.utolima.vehiculosdocumentosapi.security.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity // activa la seguridad personalizada -- en cuanto existe este @Bean, Spring Boot DEJA de generar la contrasena aleatoria de antes
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ApiKeyFilter apiKeyFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // API REST sin sesiones/cookies ni formularios HTML -- CSRF no aplica aqui
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // nunca guarda sesion: cada peticion se autentica sola, con su propio JWT
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/publico/**", "/api/auth/**").permitAll() // EXACTAMENTE lo que pide el PDF
                .anyRequest().authenticated() // todo lo demas, Entrega 1 incluida, exige estar autenticado
            )
            .exceptionHandling(ex -> ex.authenticationEntryPoint(customAuthenticationEntryPoint))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class) // 1ro: valida el JWT
            .addFilterAfter(apiKeyFilter, JwtAuthenticationFilter.class);                          // 2do: valida el APIKey

        return http.build();
    }
}
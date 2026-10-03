package com.utolima.vehiculosdocumentosapi.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.utolima.vehiculosdocumentosapi.repository.UsuarioRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component // OncePerRequestFilter se registra como bean para que SecurityConfig lo pueda inyectar despues
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        // si no hay header, o no empieza con "Bearer ", NO hacemos nada aqui
        // dejamos que SecurityConfig decida mas adelante si esa ruta necesita autenticacion (las publicas no)
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7); // quita el prefijo "Bearer " (7 caracteres exactos)

            if (jwtUtil.esTokenValido(token)) {
                String login = jwtUtil.extraerLogin(token);

                usuarioRepository.findByIdLogin(login).ifPresent(usuario -> {
                    // "principal = usuario completo", lo necesitamos asi (no solo el login) para que
                    // ApiKeyFilter pueda leer usuario.getApikey() sin consultar la base de datos otra vez
                    var authentication = new UsernamePasswordAuthenticationToken(
                            usuario, null, List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRATIVO")));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
            }
            // si el token es invalido (expirado, mal firmado): no autenticamos a nadie, pero NO cortamos la peticion aqui 
            // sigue de largo sin credenciales, y mas adelante SecurityConfig la rechaza con 401 si la ruta lo exige
        }

        filterChain.doFilter(request, response); // SIEMPRE continua la cadena de filtros, autenticado o no
    }
}
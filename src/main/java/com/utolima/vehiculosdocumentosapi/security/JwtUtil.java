package com.utolima.vehiculosdocumentosapi.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component // bean de Spring, se puede inyectar donde se necesite
public class JwtUtil {

    @Value("${jwt.secret}") // lee el valor directo de application.properties
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey getSigningKey() {
        // convierte el String de la propiedad en una clave criptografica utilizable por la libreria
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generarToken(String login) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + expirationMs);

        return Jwts.builder()
                .subject(login)       // el "dueño" del token -- luego lo recuperamos con extraerLogin()
                .issuedAt(ahora)       // cuando se genero
                .expiration(expiracion) // cuando deja de ser valido
                .signWith(getSigningKey()) // firma el token: sin la clave correcta, nadie puede alterarlo sin que se note
                .compact();            // arma el String final del token
    }

    public String extraerLogin(String token) {
        return parseToken(token).getSubject();
    }

    public boolean esTokenValido(String token) {
        try {
            parseToken(token); // si el token esta corrupto, expirado, o mal firmado, esto lanza una excepcion
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey()) // verifica que la firma coincida con nuestra clave secreta
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
package com.clinica.psicologia.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private static final String TIPO_ACCESS = "access";
    private static final String TIPO_REFRESH = "refresh";

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private Key getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String username, String rol) {
        return generateToken(username, rol, TIPO_ACCESS, expirationMs);
    }

    public String generateRefreshToken(String username, String rol) {
        return generateToken(username, rol, TIPO_REFRESH, refreshExpirationMs);
    }

    private String generateToken(String username, String rol, String tipo, long duracionMs) {
        return Jwts.builder()
                .setSubject(username)
                .claim("rol", rol)
                .claim("tipo", tipo)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + duracionMs))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    public String extractRol(String token) {
        return (String) getClaims(token).get("rol");
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean validateAccessToken(String token) {
        return validateTokenType(token, TIPO_ACCESS);
    }

    public boolean validateRefreshToken(String token) {
        return validateTokenType(token, TIPO_REFRESH);
    }

    private boolean validateTokenType(String token, String tipoEsperado) {
        try {
            Claims claims = getClaims(token);
            String tipo = claims.get("tipo", String.class);
            return tipoEsperado.equals(tipo);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}

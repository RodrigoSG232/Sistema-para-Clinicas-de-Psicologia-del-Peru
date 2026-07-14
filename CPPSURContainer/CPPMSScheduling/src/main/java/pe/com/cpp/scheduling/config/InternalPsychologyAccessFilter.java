package pe.com.cpp.scheduling.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class InternalPsychologyAccessFilter extends OncePerRequestFilter {

    public static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";
    public static final String AUTHENTICATED_USERNAME_HEADER = "X-Authenticated-Username";
    private static final String INTERNAL_PSYCHOLOGY_PATH = "/api/scheduling/internal/psychology/";

    private final byte[] apiKey;

    public InternalPsychologyAccessFilter(
            @Value("${internal.api.key:cpp-internal-dev-key}") String apiKey) {
        this.apiKey = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(INTERNAL_PSYCHOLOGY_PATH);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String providedKey = request.getHeader(INTERNAL_API_KEY_HEADER);
        if (providedKey == null || !MessageDigest.isEqual(
                apiKey, providedKey.getBytes(StandardCharsets.UTF_8))) {
            forbidden(response, "Acceso interno no autorizado");
            return;
        }

        String authenticatedUsername = request.getHeader(AUTHENTICATED_USERNAME_HEADER);
        if (authenticatedUsername == null || authenticatedUsername.isBlank()) {
            forbidden(response, "No se recibió la identidad autenticada");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void forbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}

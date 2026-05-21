package com.odontoflow.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Component
public class WhatsAppApiKeyFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-API-Key";
    private static final String PATH_PREFIX = "/webhook/whatsapp/";

    private final byte[] expectedKeyBytes;
    private final boolean configured;

    public WhatsAppApiKeyFilter(@Value("${app.whatsapp.api-key:}") String expectedKey) {
        this.configured = expectedKey != null && !expectedKey.isBlank();
        this.expectedKeyBytes = configured
                ? expectedKey.getBytes(StandardCharsets.UTF_8)
                : new byte[0];
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().contains(PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        if (!configured) {
            writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "WhatsApp integration not configured");
            return;
        }

        if (!keysMatch(request.getHeader(HEADER))) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid API key");
            return;
        }

        var auth = new UsernamePasswordAuthenticationToken(
                "system-whatsapp",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_SYSTEM"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }

    private boolean keysMatch(String provided) {
        if (provided == null) return false;
        byte[] providedBytes = provided.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(providedBytes, expectedKeyBytes);
    }

    private void writeError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json; charset=utf-8");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
        response.getWriter().flush();
    }
}

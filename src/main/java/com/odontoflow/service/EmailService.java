package com.odontoflow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Slf4j
@Service
public class EmailService {

    @Value("${resend.api-key:}")
    private String apiKey;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public void sendPasswordResetEmail(String toEmail, String token) {
        String resetLink = frontendUrl + "/redefinir-senha?token=" + token;

        String jsonBody = """
                {
                    "from": "OdontoFlow <onboarding@resend.dev>",
                    "to": ["%s"],
                    "subject": "OdontoFlow — Redefinição de senha",
                    "html": "<div style='font-family:Arial,sans-serif;max-width:480px;margin:0 auto;padding:32px'><h2 style='color:#0d9488'>Redefinição de Senha</h2><p>Você solicitou a redefinição da sua senha no OdontoFlow.</p><p>Clique no botão abaixo para criar uma nova senha:</p><a href='%s' style='display:inline-block;background:#0d9488;color:white;padding:12px 24px;border-radius:8px;text-decoration:none;font-weight:bold;margin:16px 0'>Redefinir Senha</a><p style='color:#6b7280;font-size:14px'>Este link expira em 30 minutos.</p><p style='color:#6b7280;font-size:14px'>Se você não solicitou esta redefinição, ignore este e-mail.</p></div>"
                }
                """.formatted(toEmail, resetLink);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.resend.com/emails"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                log.error("Falha ao enviar e-mail via Resend: {} - {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Erro ao enviar e-mail de redefinição de senha", e);
        }
    }
}

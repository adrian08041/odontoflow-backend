package com.odontoflow.controller;

import com.odontoflow.dto.request.ReminderResponseRequest;
import com.odontoflow.dto.response.PendingReminderResponse;
import com.odontoflow.service.WhatsAppReminderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/webhook/whatsapp")
@RequiredArgsConstructor
@Tag(name = "WhatsApp Webhook", description = "Integração M2M com n8n para lembretes via WhatsApp")
public class WhatsAppWebhookController {

    private final WhatsAppReminderService service;

    @GetMapping("/pending-reminders")
    public ResponseEntity<List<PendingReminderResponse>> pendingReminders() {
        return ResponseEntity.ok(service.findPendingReminders());
    }

    @PostMapping("/appointments/{id}/reminder-response")
    public ResponseEntity<Void> reminderResponse(
            @PathVariable UUID id,
            @Valid @RequestBody ReminderResponseRequest request) {
        service.applyReminderResponse(id, request.action(), request.respondedAt());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/appointments/{id}/reminder-sent")
    public ResponseEntity<Void> reminderSent(@PathVariable UUID id) {
        service.markReminderSent(id);
        return ResponseEntity.ok().build();
    }
}

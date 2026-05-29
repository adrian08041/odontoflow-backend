package com.odontoflow.controller;

import com.odontoflow.dto.request.BotAppointmentRequest;
import com.odontoflow.dto.request.ConversationMessageRequest;
import com.odontoflow.dto.request.HandoffRequest;
import com.odontoflow.dto.response.AvailabilitySlotResponse;
import com.odontoflow.dto.response.ConversationMessageResponse;
import com.odontoflow.dto.response.HandoffResponse;
import com.odontoflow.dto.response.PatientContextResponse;
import com.odontoflow.entity.Appointment;
import com.odontoflow.service.ConversationMessageService;
import com.odontoflow.service.AvailabilityService;
import com.odontoflow.service.WhatsAppBotService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/webhook/whatsapp")
@RequiredArgsConstructor
@Tag(name = "WhatsApp Bot (Fase 2)",
     description = "Endpoints consumidos pelo AI Agent no n8n: contexto, disponibilidade, agendamento e memória")
public class WhatsAppBotController {

    private final WhatsAppBotService botService;
    private final AvailabilityService availabilityService;
    private final ConversationMessageService conversationService;

    @GetMapping("/patient-context")
    public ResponseEntity<PatientContextResponse> patientContext(@RequestParam String phone) {
        return ResponseEntity.ok(botService.getPatientContext(phone));
    }

    @GetMapping("/availability")
    public ResponseEntity<List<AvailabilitySlotResponse>> availability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID dentistId,
            @RequestParam(required = false) Integer durationMin) {
        return ResponseEntity.ok(
                availabilityService.findAvailability(from, to, dentistId, durationMin));
    }

    @PostMapping("/appointments")
    public ResponseEntity<Map<String, Object>> createAppointment(
            @Valid @RequestBody BotAppointmentRequest req) {
        Appointment a = botService.createAppointment(req);
        return ResponseEntity.ok(Map.of(
                "appointmentId", a.getId(),
                "status", a.getStatus(),
                "date", a.getDate(),
                "time", a.getTime(),
                "dentistName", a.getDentist().getName(),
                "patientName", a.getPatientName()
        ));
    }

    @PostMapping("/appointments/{id}/cancel")
    public ResponseEntity<Void> cancelAppointment(@PathVariable UUID id) {
        botService.cancelAppointment(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/appointments/{id}/reschedule-request")
    public ResponseEntity<Void> requestReschedule(@PathVariable UUID id) {
        botService.requestReschedule(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/conversations/{phone}/messages")
    public ResponseEntity<ConversationMessageResponse> appendMessage(
            @PathVariable String phone,
            @Valid @RequestBody ConversationMessageRequest req) {
        var msg = conversationService.append(phone, req.role(), req.content(), req.toolName());
        return ResponseEntity.ok(ConversationMessageResponse.from(msg));
    }

    @GetMapping("/conversations/{phone}/messages")
    public ResponseEntity<List<ConversationMessageResponse>> recentMessages(@PathVariable String phone) {
        var list = conversationService.listRecent(phone).stream()
                .map(ConversationMessageResponse::from)
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/handoff")
    public ResponseEntity<HandoffResponse> handoff(@Valid @RequestBody HandoffRequest req) {
        return ResponseEntity.ok(botService.handoff(req));
    }
}

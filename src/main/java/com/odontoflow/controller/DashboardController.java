package com.odontoflow.controller;

import com.odontoflow.dto.response.DashboardAlertResponse;
import com.odontoflow.dto.response.DashboardAppointment;
import com.odontoflow.dto.response.DashboardGoalsResponse;
import com.odontoflow.dto.response.DashboardSummaryResponse;
import com.odontoflow.dto.response.WeeklyChartResponse;
import com.odontoflow.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Métricas e indicadores da tela inicial")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "Cards resumo", description = "Consultas de hoje, confirmadas, faturamento do mês, novos pacientes e crescimento vs mês anterior")
    @ApiResponse(responseCode = "200", description = "Resumo retornado")
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    @GetMapping("/today-agenda")
    @Operation(summary = "Agenda de hoje", description = "Lista de agendamentos do dia atual com nome do paciente, dentista e status")
    @ApiResponse(responseCode = "200", description = "Agenda retornada")
    public ResponseEntity<List<DashboardAppointment>> getTodayAgenda() {
        return ResponseEntity.ok(dashboardService.getTodayAgenda());
    }

    @GetMapping("/goals")
    @Operation(summary = "Metas do mês", description = "Faturamento e tratamentos concluídos vs metas")
    @ApiResponse(responseCode = "200", description = "Metas retornadas")
    public ResponseEntity<DashboardGoalsResponse> getGoals() {
        return ResponseEntity.ok(dashboardService.getGoals());
    }

    @GetMapping("/alerts")
    @Operation(summary = "Alertas dinâmicos", description = "Não confirmados de amanhã, pagamentos vencidos, aniversariantes e retornos pendentes — calculados em tempo real")
    @ApiResponse(responseCode = "200", description = "Alertas retornados")
    public ResponseEntity<List<DashboardAlertResponse>> getAlerts() {
        return ResponseEntity.ok(dashboardService.getAlerts());
    }

    @GetMapping("/weekly-chart")
    @Operation(summary = "Consultas da semana", description = "Distribuição de consultas por dia da semana (segunda a domingo)")
    @ApiResponse(responseCode = "200", description = "Gráfico retornado")
    public ResponseEntity<WeeklyChartResponse> getWeeklyChart() {
        return ResponseEntity.ok(dashboardService.getWeeklyChart());
    }
}

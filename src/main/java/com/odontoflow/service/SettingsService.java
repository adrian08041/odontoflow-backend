package com.odontoflow.service;

import com.odontoflow.dto.request.ClinicHourRequest;
import com.odontoflow.dto.request.ClinicRequest;
import com.odontoflow.dto.request.HoursRequest;
import com.odontoflow.dto.request.InsuranceRequest;
import com.odontoflow.dto.response.ClinicResponse;
import com.odontoflow.dto.response.HoursResponse;
import com.odontoflow.dto.response.InsuranceResponse;
import com.odontoflow.entity.Clinic;
import com.odontoflow.entity.ClinicHour;
import com.odontoflow.entity.Insurance;
import com.odontoflow.exception.BusinessException;
import com.odontoflow.repository.ClinicRepository;
import com.odontoflow.repository.InsuranceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private static final List<String> DEFAULT_DAYS = List.of(
            "Segunda-feira", "Terça-feira", "Quarta-feira",
            "Quinta-feira", "Sexta-feira", "Sábado", "Domingo"
    );

    private static final Pattern HH_MM = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");

    private final ClinicRepository clinicRepository;
    private final InsuranceRepository insuranceRepository;

    @Transactional
    public Clinic ensureDefaults() {
        return clinicRepository.findFirstByOrderByCreatedAtAsc()
                .orElseGet(this::createDefaultClinic);
    }

    @Transactional(readOnly = true)
    public ClinicResponse getClinic() {
        return ClinicResponse.from(getOrThrow());
    }

    @Transactional
    public ClinicResponse updateClinic(ClinicRequest request) {
        validateClinic(request);
        Clinic clinic = getOrThrow();
        clinic.setNomeFantasia(request.getNomeFantasia().trim());
        clinic.setCnpj(onlyDigits(request.getCnpj()));
        clinic.setTelefone(onlyDigits(request.getTelefone()));
        clinic.setEndereco(request.getEndereco().trim());
        clinic.setEmail(request.getEmail().trim());
        clinic.setWebsite(request.getWebsite());
        if (request.getLogoUrl() != null) {
            clinic.setLogoUrl(request.getLogoUrl());
        }
        return ClinicResponse.from(clinicRepository.save(clinic));
    }

    @Transactional(readOnly = true)
    public HoursResponse getHours() {
        return HoursResponse.from(getOrThrow());
    }

    @Transactional
    public HoursResponse updateHours(HoursRequest request) {
        validateHours(request);

        Clinic clinic = getOrThrow();
        clinic.setDuracaoConsulta(request.getDuracaoConsulta());
        clinic.setIntervalo(request.getIntervalo());

        clinic.getHours().clear();
        List<ClinicHourRequest> dias = request.getDias();
        for (int i = 0; i < dias.size(); i++) {
            ClinicHourRequest d = dias.get(i);
            ClinicHour h = new ClinicHour();
            h.setClinic(clinic);
            h.setLabel(d.getLabel().trim());
            h.setActive(Boolean.TRUE.equals(d.getActive()));
            h.setStart(blankToNull(d.getStart()));
            h.setEnd(blankToNull(d.getEnd()));
            h.setPosition(i);
            clinic.getHours().add(h);
        }

        return HoursResponse.from(clinicRepository.save(clinic));
    }

    @Transactional(readOnly = true)
    public List<InsuranceResponse> listInsurances() {
        return insuranceRepository.findAllByOrderByNameAsc()
                .stream().map(InsuranceResponse::from).toList();
    }

    @Transactional
    public InsuranceResponse addInsurance(InsuranceRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BusinessException("O nome do convênio é obrigatório.");
        }
        Insurance i = new Insurance();
        i.setName(request.getName().trim());
        i.setCode(request.getCode());
        i.setType(request.getType());
        i.setDiscount(request.getDiscount());
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            i.setStatus(request.getStatus());
        }
        return InsuranceResponse.from(insuranceRepository.save(i));
    }

    @Transactional
    public InsuranceResponse updateInsurance(UUID id, InsuranceRequest request) {
        Insurance i = insuranceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Convênio não encontrado."));
        if (request.getName() != null && !request.getName().isBlank()) {
            i.setName(request.getName().trim());
        }
        if (request.getCode() != null) i.setCode(request.getCode());
        if (request.getType() != null) i.setType(request.getType());
        if (request.getDiscount() != null) i.setDiscount(request.getDiscount());
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            i.setStatus(request.getStatus());
        }
        return InsuranceResponse.from(insuranceRepository.save(i));
    }

    @Transactional
    public void deleteInsurance(UUID id) {
        if (!insuranceRepository.existsById(id)) {
            throw new BusinessException("Convênio não encontrado.");
        }
        insuranceRepository.deleteById(id);
    }

    private Clinic getOrThrow() {
        return clinicRepository.findFirstByOrderByCreatedAtAsc()
                .orElseThrow(() -> new BusinessException("Clínica não inicializada. Reinicie o servidor."));
    }

    private Clinic createDefaultClinic() {
        Clinic c = new Clinic();
        c.setNomeFantasia("Minha Clínica");
        c.setCnpj("00000000000000");
        c.setTelefone("0000000000");
        c.setEndereco("Endereço a configurar");
        c.setEmail("contato@clinica.com");
        c.setDuracaoConsulta("30 min");
        c.setIntervalo("15 min");

        for (int i = 0; i < DEFAULT_DAYS.size(); i++) {
            ClinicHour h = new ClinicHour();
            h.setClinic(c);
            h.setLabel(DEFAULT_DAYS.get(i));
            h.setActive(i < 5);
            h.setStart(i < 5 ? "08:00" : null);
            h.setEnd(i < 5 ? "18:00" : null);
            h.setPosition(i);
            c.getHours().add(h);
        }
        return clinicRepository.save(c);
    }

    private String onlyDigits(String value) {
        return value == null ? null : value.replaceAll("\\D", "");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validateClinic(ClinicRequest r) {
        if (r.getNomeFantasia() == null || r.getNomeFantasia().trim().length() < 2) {
            throw new BusinessException("Nome fantasia inválido (mínimo 2 caracteres).");
        }
        if (r.getCnpj() == null || r.getCnpj().replaceAll("\\D", "").length() < 14) {
            throw new BusinessException("CNPJ inválido (mínimo 14 dígitos).");
        }
        if (r.getTelefone() == null || r.getTelefone().replaceAll("\\D", "").length() < 10) {
            throw new BusinessException("Telefone inválido (mínimo 10 dígitos).");
        }
        if (r.getEndereco() == null || r.getEndereco().trim().length() < 10) {
            throw new BusinessException("Endereço incompleto (mínimo 10 caracteres).");
        }
        if (r.getEmail() == null || !r.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new BusinessException("E-mail inválido.");
        }
    }

    private void validateHours(HoursRequest request) {
        if (request.getDias() == null || request.getDias().isEmpty()) {
            throw new BusinessException("Informe a lista de dias da semana.");
        }
        if (request.getDuracaoConsulta() == null || request.getDuracaoConsulta().isBlank()) {
            throw new BusinessException("A duração da consulta é obrigatória.");
        }
        if (request.getIntervalo() == null || request.getIntervalo().isBlank()) {
            throw new BusinessException("O intervalo é obrigatório.");
        }
        for (ClinicHourRequest d : request.getDias()) {
            if (d.getLabel() == null || d.getLabel().isBlank()) {
                throw new BusinessException("Todo dia precisa de um label.");
            }
            String start = blankToNull(d.getStart());
            String end = blankToNull(d.getEnd());
            boolean active = Boolean.TRUE.equals(d.getActive());

            if (active && (start == null || end == null)) {
                throw new BusinessException(
                        "Dia ativo (" + d.getLabel() + ") precisa de horário de início e fim.");
            }
            if (start != null && !HH_MM.matcher(start).matches()) {
                throw new BusinessException(
                        "Horário de início inválido em " + d.getLabel() + " (use HH:mm).");
            }
            if (end != null && !HH_MM.matcher(end).matches()) {
                throw new BusinessException(
                        "Horário de fim inválido em " + d.getLabel() + " (use HH:mm).");
            }
            if (start != null && end != null && start.compareTo(end) >= 0) {
                throw new BusinessException(
                        "Em " + d.getLabel() + ", o horário de início deve ser anterior ao fim.");
            }
        }
    }
}

package com.odontoflow.dto.response;

import com.odontoflow.entity.Clinic;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClinicResponse {
    private UUID id;
    private String nomeFantasia;
    private String cnpj;
    private String telefone;
    private String endereco;
    private String email;
    private String website;
    private String logoUrl;
    private BigDecimal revenueGoal;
    private Long treatmentGoal;

    public static ClinicResponse from(Clinic c) {
        return new ClinicResponse(
                c.getId(),
                c.getNomeFantasia(),
                c.getCnpj(),
                c.getTelefone(),
                c.getEndereco(),
                c.getEmail(),
                c.getWebsite(),
                c.getLogoUrl(),
                c.getRevenueGoal(),
                c.getTreatmentGoal()
        );
    }
}

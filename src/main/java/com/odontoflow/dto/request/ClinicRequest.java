package com.odontoflow.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ClinicRequest {
    private String nomeFantasia;
    private String cnpj;
    private String telefone;
    private String endereco;
    private String email;
    private String website;
    private String logoUrl;
    private BigDecimal revenueGoal;
    private Long treatmentGoal;
}

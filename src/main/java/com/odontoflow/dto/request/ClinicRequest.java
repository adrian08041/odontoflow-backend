package com.odontoflow.dto.request;

import lombok.Getter;
import lombok.Setter;

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
}

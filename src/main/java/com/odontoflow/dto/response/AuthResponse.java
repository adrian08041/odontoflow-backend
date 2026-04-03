package com.odontoflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private UUID id;
    private String name;
    private String email;
    private String role;
    private String initials;
}

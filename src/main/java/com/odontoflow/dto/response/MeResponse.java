package com.odontoflow.dto.response;

import com.odontoflow.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class MeResponse {
    private UUID id;
    private String name;
    private String email;
    private String role;
    private String initials;
    private String avatarUrl;

    public static MeResponse from(User user) {
        return new MeResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getInitials(),
                user.getAvatarUrl()
        );
    }
}

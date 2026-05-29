package com.odontoflow.service;

import com.odontoflow.config.JwtTokenProvider;
import com.odontoflow.dto.request.UpdateMeRequest;
import com.odontoflow.dto.request.UpdatePasswordRequest;
import com.odontoflow.dto.response.AuthResponse;
import com.odontoflow.dto.response.MeResponse;
import com.odontoflow.dto.response.MessageResponse;
import com.odontoflow.entity.User;
import com.odontoflow.entity.enums.AuditAction;
import com.odontoflow.exception.BusinessException;
import com.odontoflow.repository.UserRepository;
import com.odontoflow.util.AuditChanges;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final long MAX_AVATAR_BYTES = 2L * 1024 * 1024;
    private static final Set<String> AVATAR_TYPES = Set.of(
            "image/png", "image/jpeg", "image/jpg", "image/webp"
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final SupabaseStorageService storageService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public MeResponse getMe() {
        return MeResponse.from(currentUser());
    }

    @Transactional
    public AuthResponse updateMe(UpdateMeRequest request) {
        User user = currentUser();
        Map<String, Object> before = AuditChanges.snapshot(user);

        String email = request.getEmail().trim().toLowerCase();
        userRepository.findByEmail(email).ifPresent(other -> {
            if (!other.getId().equals(user.getId())) {
                throw new BusinessException("E-mail já cadastrado por outro usuário.");
            }
        });

        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setInitials(generateInitials(user.getName()));
        User saved = userRepository.save(user);

        auditLogService.log("User", saved.getId(), AuditAction.UPDATE,
                AuditChanges.diff(before, AuditChanges.snapshot(saved)));

        // Reemite o JWT: ele carrega name/email (usados no audit). Sem reemitir, o
        // log mostraria o nome antigo até o próximo login.
        String token = jwtTokenProvider.generateToken(
                saved.getId(), saved.getEmail(), saved.getRole().name(), saved.getName());

        return new AuthResponse(token, saved.getId(), saved.getName(), saved.getEmail(),
                saved.getRole().name(), saved.getInitials(), saved.getAvatarUrl());
    }

    @Transactional
    public MessageResponse updatePassword(UpdatePasswordRequest request) {
        User user = currentUser();
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("Senha atual incorreta.");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return new MessageResponse("Senha alterada com sucesso.");
    }

    @Transactional
    public MeResponse uploadAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Envie um arquivo de imagem.");
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new BusinessException("A foto excede o tamanho máximo de 2 MB.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !AVATAR_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException("Tipo inválido para foto. Aceitos: PNG, JPG, JPEG, WEBP.");
        }

        User user = currentUser();
        deleteOldAvatar(user);

        String ext = switch (contentType.toLowerCase()) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
        String path = "avatars/" + user.getId() + "/" + UUID.randomUUID() + "." + ext;

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw new BusinessException("Falha ao ler o arquivo enviado.");
        }
        String publicUrl = storageService.upload(bytes, path, contentType);
        user.setAvatarUrl(publicUrl);
        User saved = userRepository.save(user);

        auditLogService.log("User", saved.getId(), AuditAction.UPDATE,
                Map.of("field", "avatarUrl", "avatarUrl", publicUrl));
        return MeResponse.from(saved);
    }

    @Transactional
    public MeResponse deleteAvatar() {
        User user = currentUser();
        deleteOldAvatar(user);
        user.setAvatarUrl(null);
        User saved = userRepository.save(user);

        auditLogService.log("User", saved.getId(), AuditAction.UPDATE,
                Map.of("field", "avatarUrl", "avatarUrl", "removido"));
        return MeResponse.from(saved);
    }

    // ---------- internos ----------

    private void deleteOldAvatar(User user) {
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isBlank()) {
            String oldPath = extractPathFromUrl(user.getAvatarUrl());
            if (oldPath != null) storageService.delete(oldPath);
        }
    }

    private String extractPathFromUrl(String url) {
        int idx = url.indexOf("/object/public/");
        if (idx < 0) return null;
        int after = url.indexOf("/", idx + "/object/public/".length());
        return after < 0 ? null : url.substring(after + 1);
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UUID userId)) {
            throw new BusinessException("Usuário não autenticado.");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado."));
    }

    private String generateInitials(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
        }
        return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
    }
}

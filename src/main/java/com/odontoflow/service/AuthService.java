package com.odontoflow.service;

import com.odontoflow.config.JwtTokenProvider;
import com.odontoflow.dto.request.ForgotPasswordRequest;
import com.odontoflow.dto.request.LoginRequest;
import com.odontoflow.dto.request.RegisterRequest;
import com.odontoflow.dto.request.ResetPasswordRequest;
import com.odontoflow.dto.response.AuthResponse;
import com.odontoflow.dto.response.MessageResponse;
import com.odontoflow.entity.PasswordResetToken;
import com.odontoflow.entity.User;
import com.odontoflow.entity.enums.UserRole;
import com.odontoflow.exception.BusinessException;
import com.odontoflow.repository.PasswordResetTokenRepository;
import com.odontoflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("E-mail ou senha inválidos"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("E-mail ou senha inválidos");
        }

        if (!user.getActive()) {
            throw new BusinessException("Usuário desativado");
        }

        return buildAuthResponse(user);
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BusinessException("E-mail já cadastrado");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.RECEPCIONISTA);
        user.setInitials(generateInitials(request.getName()));

        User savedUser = userRepository.save(user);

        return buildAuthResponse(savedUser);
    }

    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            passwordResetTokenRepository.deleteByUser(user);

            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = new PasswordResetToken(
                    token, user, LocalDateTime.now().plusMinutes(30));
            passwordResetTokenRepository.save(resetToken);

            emailService.sendPasswordResetEmail(user.getEmail(), token);
        });

        return new MessageResponse("Se o e-mail estiver cadastrado, você receberá instruções para redefinir sua senha.");
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenAndUsedFalse(request.getToken())
                .orElseThrow(() -> new BusinessException("Token inválido ou já utilizado"));

        if (resetToken.isExpired()) {
            throw new BusinessException("Token expirado. Solicite uma nova redefinição de senha.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        return new MessageResponse("Senha redefinida com sucesso.");
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtTokenProvider.generateToken(
                user.getId(), user.getEmail(), user.getRole().name());

        return new AuthResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getInitials()
        );
    }

    private String generateInitials(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
        }
        return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
    }
}

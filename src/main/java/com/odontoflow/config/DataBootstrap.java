package com.odontoflow.config;

import com.odontoflow.entity.User;
import com.odontoflow.entity.enums.UserRole;
import com.odontoflow.repository.UserRepository;
import com.odontoflow.service.SettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataBootstrap implements CommandLineRunner {

    private static final String DEFAULT_ADMIN_EMAIL = "teste@odontoflow.com";
    private static final String DEFAULT_ADMIN_PASSWORD = "12345678";

    private final SettingsService settingsService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        settingsService.ensureDefaults();
        ensureDefaultAdmin();
    }

    /**
     * Garante acesso inicial criando um ADMIN default quando não há nenhum usuário.
     * Substitui o antigo auto-cadastro público (POST /auth/register), removido por
     * permitir que qualquer pessoa se tornasse operador da clínica.
     */
    private void ensureDefaultAdmin() {
        if (userRepository.count() > 0) {
            return;
        }
        User admin = new User();
        admin.setName("Administrador");
        admin.setEmail(DEFAULT_ADMIN_EMAIL);
        admin.setPassword(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD));
        admin.setRole(UserRole.ADMIN);
        admin.setInitials("AD");
        userRepository.save(admin);
        log.warn("Nenhum usuário encontrado — ADMIN default criado ({}). "
                + "Troque a senha após o primeiro acesso.", DEFAULT_ADMIN_EMAIL);
    }
}

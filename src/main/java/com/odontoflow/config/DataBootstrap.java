package com.odontoflow.config;

import com.odontoflow.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataBootstrap implements CommandLineRunner {

    private final SettingsService settingsService;

    @Override
    public void run(String... args) {
        settingsService.ensureDefaults();
    }
}

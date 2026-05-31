package com.contenthub.config;

import com.contenthub.entity.User;
import com.contenthub.entity.UserSettings;
import com.contenthub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername(adminUsername).isEmpty()) {
            String finalPassword = adminPassword;
            if (finalPassword == null || finalPassword.isBlank()) {
                finalPassword = UUID.randomUUID().toString().substring(0, 12);
                log.warn("未设置管理员密码，已生成随机密码: {}", finalPassword);
            }
            User admin = User.builder()
                    .username(adminUsername)
                    .email(adminEmail)
                    .password(passwordEncoder.encode(finalPassword))
                    .bio("管理员账号")
                    .build();
            UserSettings settings = UserSettings.builder().user(admin).build();
            admin.setSettings(settings);
            userRepository.save(admin);
            log.info("管理员账号已创建: {}", adminUsername);
        }
    }
}

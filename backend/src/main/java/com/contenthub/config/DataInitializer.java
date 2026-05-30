package com.contenthub.config;

import com.contenthub.entity.User;
import com.contenthub.entity.UserSettings;
import com.contenthub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@contenthub.com")
                    .password(passwordEncoder.encode("123456"))
                    .bio("管理员账号")
                    .build();
            UserSettings settings = UserSettings.builder().user(admin).build();
            admin.setSettings(settings);
            userRepository.save(admin);
        }
    }
}

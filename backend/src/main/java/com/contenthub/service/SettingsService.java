package com.contenthub.service;

import com.contenthub.dto.UserSettingsRequest;
import com.contenthub.dto.UserSettingsResponse;
import com.contenthub.entity.User;
import com.contenthub.entity.UserSettings;
import com.contenthub.exception.ResourceNotFoundException;
import com.contenthub.repository.UserRepository;
import com.contenthub.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final UserSettingsRepository userSettingsRepository;
    private final UserRepository userRepository;

    public UserSettingsResponse getSettings(Long userId) {
        UserSettings settings = userSettingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));
        return toResponse(settings);
    }

    @Transactional
    public UserSettingsResponse updateSettings(UserSettingsRequest request, Long userId) {
        UserSettings settings = userSettingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));

        if (request.getDefaultPlatform() != null) settings.setDefaultPlatform(request.getDefaultPlatform());
        if (request.getAutoPublish() != null) settings.setAutoPublish(request.getAutoPublish());
        if (request.getPublishInterval() != null) settings.setPublishInterval(request.getPublishInterval());
        if (request.getEmailNotification() != null) settings.setEmailNotification(request.getEmailNotification());
        if (request.getPushNotification() != null) settings.setPushNotification(request.getPushNotification());
        if (request.getPublishAutoRetry() != null) settings.setPublishAutoRetry(request.getPublishAutoRetry());
        if (request.getTheme() != null) settings.setTheme(request.getTheme());
        if (request.getLanguage() != null) settings.setLanguage(request.getLanguage());

        settings = userSettingsRepository.save(settings);
        return toResponse(settings);
    }

    private UserSettings createDefaultSettings(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        UserSettings settings = UserSettings.builder().user(user).build();
        return userSettingsRepository.save(settings);
    }

    private UserSettingsResponse toResponse(UserSettings settings) {
        return UserSettingsResponse.builder()
                .id(settings.getId())
                .defaultPlatform(settings.getDefaultPlatform())
                .autoPublish(settings.getAutoPublish())
                .publishInterval(settings.getPublishInterval())
                .emailNotification(settings.getEmailNotification())
                .pushNotification(settings.getPushNotification())
                .publishAutoRetry(settings.getPublishAutoRetry())
                .theme(settings.getTheme())
                .language(settings.getLanguage())
                .build();
    }
}

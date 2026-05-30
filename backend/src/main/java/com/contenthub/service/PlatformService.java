package com.contenthub.service;

import com.contenthub.common.Constants;
import com.contenthub.dto.PlatformAccountResponse;
import com.contenthub.entity.PlatformAccount;
import com.contenthub.entity.User;
import com.contenthub.exception.BadRequestException;
import com.contenthub.exception.ResourceNotFoundException;
import com.contenthub.repository.PlatformAccountRepository;
import com.contenthub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlatformService {

    private final PlatformAccountRepository platformAccountRepository;
    private final UserRepository userRepository;

    public List<PlatformAccountResponse> getUserPlatforms(Long userId) {
        return platformAccountRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PlatformAccountResponse bindPlatform(String platformStr, Long userId) {
        Constants.Platform platform;
        try {
            platform = Constants.Platform.valueOf(platformStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("不支持的平台: " + platformStr);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        if (platformAccountRepository.existsByUserIdAndPlatform(userId, platform)) {
            throw new BadRequestException("该平台已绑定");
        }

        PlatformAccount account = PlatformAccount.builder()
                .user(user)
                .platform(platform)
                .accountName(platform.getName() + "账号")
                .accessToken("mock-token-" + System.currentTimeMillis())
                .build();

        account = platformAccountRepository.save(account);
        return toResponse(account);
    }

    @Transactional
    public void unbindPlatform(String platformStr, Long userId) {
        Constants.Platform platform;
        try {
            platform = Constants.Platform.valueOf(platformStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("不支持的平台: " + platformStr);
        }

        if (!platformAccountRepository.existsByUserIdAndPlatform(userId, platform)) {
            throw new ResourceNotFoundException("该平台未绑定");
        }

        platformAccountRepository.deleteByUserIdAndPlatform(userId, platform);
    }

    private PlatformAccountResponse toResponse(PlatformAccount account) {
        return PlatformAccountResponse.builder()
                .id(account.getId())
                .platform(account.getPlatform().name())
                .platformName(account.getPlatform().getName())
                .accountName(account.getAccountName())
                .bindTime(account.getBindTime())
                .build();
    }
}

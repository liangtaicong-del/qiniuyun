package com.contenthub.platform;

import com.contenthub.entity.Article;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 通用平台发布器（适用于微博、知乎、简书、CSDN、掘金、百家号、头条号）
 * 目前为模拟模式，各平台 API 接入方式差异较大，需要分别对接
 */
@Slf4j
@Component
public class GenericPublisher implements PlatformPublisher {

    private static final Pattern IMG_PATTERN = Pattern.compile("<img[^>]+src=[\"']([^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE);

    @Override
    public PublishResult publish(Article article, String accessToken) {
        if (accessToken == null || accessToken.startsWith("mock-")) {
            return PublishResult.failure("该平台尚未配置真实 API 凭据，目前为演示模式");
        }
        return PublishResult.failure("该平台 API 尚未接入");
    }

    @Override
    public String uploadImage(byte[] imageBytes, String filename, String accessToken) throws java.io.IOException {
        return null;
    }

    @Override
    public String convertContent(String html) {
        if (html == null) return "";
        return html.replaceAll("(?i)<img([^>]*)src=\"([^\"]+)\"", "<img$1src=\"$2\"");
    }

    @Override
    public String getPlatformName() {
        return "通用平台";
    }
}

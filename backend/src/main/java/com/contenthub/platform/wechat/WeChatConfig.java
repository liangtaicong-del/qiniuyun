package com.contenthub.platform.wechat;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.platform.wechat")
public class WeChatConfig {

    /**
     * 微信公众号 AppID（从微信公众平台获取）
     * 环境变量: WECHAT_APP_ID
     */
    private String appId;

    /**
     * 微信公众号 AppSecret（从微信公众平台获取）
     * 环境变量: WECHAT_APP_SECRET
     */
    private String appSecret;

    /**
     * 是否启用真实发布模式
     * true: 使用真实微信 API
     * false: 使用模拟模式（默认）
     * 环境变量: WECHAT_ENABLED
     */
    private boolean enabled = false;

    public boolean isConfigured() {
        return appId != null && !appId.isBlank()
            && appSecret != null && !appSecret.isBlank();
    }
}

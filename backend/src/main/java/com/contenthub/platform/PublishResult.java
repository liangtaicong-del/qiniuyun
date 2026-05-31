package com.contenthub.platform;

/**
 * 平台发布结果
 */
public record PublishResult(
        boolean success,
        String platformUrl,
        String errorMessage
) {
    public static PublishResult success(String url) {
        return new PublishResult(true, url, null);
    }

    public static PublishResult failure(String error) {
        return new PublishResult(false, null, error);
    }
}

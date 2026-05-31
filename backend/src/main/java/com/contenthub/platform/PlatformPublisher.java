package com.contenthub.platform;

import com.contenthub.entity.Article;

/**
 * 平台发布器接口
 * 各平台实现此接口以提供真实的 API 发布能力
 */
public interface PlatformPublisher {

    /**
     * 发布文章到目标平台
     *
     * @param article 文章内容
     * @param accessToken 平台的授权 Token
     * @return 发布结果，包含成功状态、平台 URL 和错误信息
     */
    PublishResult publish(Article article, String accessToken);

    /**
     * 获取平台名称
     */
    String getPlatformName();

    /**
     * 上传图片到平台媒体库并返回 URL
     *
     * @param imageBytes 图片字节数据
     * @param filename    文件名
     * @param accessToken 授权 Token
     * @return 平台上的图片 URL
     */
    String uploadImage(byte[] imageBytes, String filename, String accessToken) throws java.io.IOException;

    /**
     * 将富文本内容转换为平台特定的 HTML 格式
     *
     * @param html 富文本 HTML 内容
     * @return 平台专用的内容格式
     */
    String convertContent(String html);
}

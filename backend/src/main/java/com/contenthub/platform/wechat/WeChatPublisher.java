package com.contenthub.platform.wechat;

import com.contenthub.entity.Article;
import com.contenthub.platform.PlatformPublisher;
import com.contenthub.platform.PublishResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

@Slf4j
@Component
public class WeChatPublisher implements PlatformPublisher {

    @Value("${app.platform.wechat.app-id:}")
    private String appId;

    @Value("${app.platform.wechat.app-secret:}")
    private String appSecret;

    private static final String API_BASE = "https://api.weixin.qq.com";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public PublishResult publish(Article article, String accessToken) {
        try {
            String draftMediaId = uploadDraft(article, accessToken);
            if (draftMediaId == null) {
                return PublishResult.failure("草稿上传失败");
            }

            String articleUrl = submitDraft(draftMediaId, accessToken);
            if (articleUrl != null) {
                log.info("微信公众号发布成功，articleId={}", article.getId());
                return PublishResult.success(articleUrl);
            }
            return PublishResult.failure("草稿提交失败");
        } catch (Exception e) {
            log.error("微信公众号发布失败: {}", e.getMessage(), e);
            return PublishResult.failure("发布失败: " + e.getMessage());
        }
    }

    /**
     * 上传草稿到微信公众平台
     * API: https://developers.weixin.qq.com/doc/offiaccount/Draft_Box_Management/Add_draft.html
     */
    public String uploadDraft(Article article, String accessToken) throws IOException, InterruptedException {
        String apiUrl = API_BASE + "/cgi-bin/draft/add?access_token=" + accessToken;

        String thumbMediaId = getThumbMediaId(accessToken);
        String content = convertContent(article.getContent());

        Map<String, Object> articleMap = new LinkedHashMap<>();
        articleMap.put("title", article.getTitle());
        articleMap.put("author", "");
        articleMap.put("digest", StringUtils.hasText(article.getSummary()) ? article.getSummary() : "");
        articleMap.put("content", content);
        articleMap.put("content_source_url", "");
        articleMap.put("thumb_media_id", thumbMediaId != null ? thumbMediaId : "");
        articleMap.put("need_open_comment", 1);
        articleMap.put("only_fans_can_comment", 0);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("articles", List.of(articleMap));

        String jsonBody = toJson(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> resp = parseJson(response.body());

        if (resp == null) {
            return null;
        }

        Integer errcode = (Integer) resp.get("errcode");
        if (errcode != null && errcode != 0) {
            log.warn("微信上传草稿 API 错误: errcode={}, errmsg={}", errcode, resp.get("errmsg"));
            return null;
        }

        Map<?, ?> mediaId = (Map<?, ?>) resp.get("media_id");
        if (mediaId != null) {
            return (String) mediaId.get("media_id");
        }
        return (String) resp.get("media_id");
    }

    /**
     * 提交草稿（发布到公众号）
     * 实际发布需要用户在微信公众平台后台审核发布
     */
    public String submitDraft(String mediaId, String accessToken) throws IOException, InterruptedException {
        String apiUrl = API_BASE + "/cgi-bin/freepublish/submit?access_token=" + accessToken;

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("media_id", mediaId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(toJson(payload)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> resp = parseJson(response.body());

        if (resp == null) return null;

        Integer errcode = (Integer) resp.get("errcode");
        if (errcode != null && errcode != 0) {
            log.warn("微信提交草稿 API 错误: errcode={}, errmsg={}", errcode, resp.get("errmsg"));
            return null;
        }

        String msgId = String.valueOf(resp.get("msg_id"));
        return "https://mp.weixin.qq.com/s?msgid=" + msgId;
    }

    @Override
    public String uploadImage(byte[] imageBytes, String filename, String accessToken) throws IOException {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException("缺少微信公众号 AccessToken，请先绑定账号");
        }

        String apiUrl = API_BASE + "/cgi-bin/media/uploadimg?access_token=" + accessToken;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "multipart/form-data")
                .POST(HttpRequest.BodyPublishers.ofByteArray(imageBytes))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("请求被中断", e);
        }
        Map<String, Object> resp = parseJson(response.body());

        if (resp != null) {
            Integer errcode = (Integer) resp.get("errcode");
            if (errcode != null && errcode != 0) {
                log.warn("微信图片上传失败: {}", resp.get("errmsg"));
            }
            return (String) resp.get("url");
        }
        return null;
    }

    @Override
    public String convertContent(String html) {
        if (html == null) return "";
        return html.replaceAll("(?i)<img", "<img class=\"img-fluid\"")
                   .replaceAll("(?i)<script[^>]*>[\\s\\S]*?</script>", "")
                   .replaceAll("(?i)<style[^>]*>[\\s\\S]*?</style>", "");
    }

    @Override
    public String getPlatformName() {
        return "微信公众号";
    }

    /**
     * 获取封面图 MediaId（需要提前在素材库上传）
     * 实际使用时需要从已上传的永久素材中获取
     */
    private String getThumbMediaId(String accessToken) {
        return null;
    }

    // ============== Token 管理 ==============

    /**
     * 获取 AccessToken（注意：需要配合微信公众号 AppID 和 AppSecret）
     * 生产环境应实现 Token 缓存，避免频繁刷新
     */
    public String getAccessToken() throws IOException, InterruptedException {
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(appSecret)) {
            return null;
        }

        String url = API_BASE + "/cgi-bin/token?grant_type=client_credential"
                + "&appid=" + appId + "&secret=" + appSecret;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> resp = parseJson(response.body());

        if (resp != null && resp.containsKey("access_token")) {
            return (String) resp.get("access_token");
        }
        return null;
    }

    // ============== JSON 工具 ==============

    private String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        var entries = map.entrySet().iterator();
        while (entries.hasNext()) {
            var entry = entries.next();
            sb.append("\"").append(entry.getKey()).append("\":");
            Object val = entry.getValue();
            if (val instanceof List) {
                sb.append("[");
                List<?> list = (List<?>) val;
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append(mapToJson((Map<?, ?>) list.get(i)));
                }
                sb.append("]");
            } else if (val instanceof String) {
                sb.append("\"").append(escapeJson((String) val)).append("\"");
            } else if (val instanceof Number) {
                sb.append(val);
            } else if (val == null) {
                sb.append("null");
            }
            if (entries.hasNext()) sb.append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    private String mapToJson(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder("{");
        var entries = map.entrySet().iterator();
        while (entries.hasNext()) {
            var entry = entries.next();
            sb.append("\"").append(entry.getKey()).append("\":");
            Object val = entry.getValue();
            if (val instanceof String) {
                sb.append("\"").append(escapeJson((String) val)).append("\"");
            } else if (val instanceof Number) {
                sb.append(val);
            } else if (val == null) {
                sb.append("null");
            }
            if (entries.hasNext()) sb.append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                 .replace("\"", "\\\"")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.error("JSON 解析失败: {}", json, e);
            return null;
        }
    }
}

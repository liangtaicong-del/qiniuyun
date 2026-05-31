package com.contenthub.service;

import com.contenthub.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    @Value("${app.upload.storage:local}")
    private String storageType;

    @Value("${app.upload.local.path:./uploads}")
    private String localPath;

    @Value("${app.upload.max-size:5242880}")
    private long maxSize;

    @Value("${app.upload.allowed-types:image/jpeg,image/png,image/gif,image/webp}")
    private String allowedTypes;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");

    public String upload(MultipartFile file) throws IOException {
        validateFile(file);

        if ("qiniu".equalsIgnoreCase(storageType)) {
            return uploadToQiniu(file);
        }
        return uploadToLocal(file);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("文件不能为空");
        }
        if (file.getSize() > maxSize) {
            throw new BadRequestException("文件大小不能超过 " + (maxSize / 1024 / 1024) + "MB");
        }

        String contentType = file.getContentType();
        List<String> allowed = Arrays.asList(allowedTypes.split(","));
        if (!allowed.contains(contentType)) {
            throw new BadRequestException("不支持的文件类型: " + contentType);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(ext)) {
                throw new BadRequestException("不支持的图片格式: " + ext);
            }
        }
    }

    private String uploadToLocal(MultipartFile file) throws IOException {
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String ext = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;

        Path uploadDir = Paths.get(localPath, dateDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);

        Path targetPath = uploadDir.resolve(filename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        String url = "/uploads/" + dateDir + "/" + filename;
        log.info("文件已上传至本地: {}", targetPath);
        return url;
    }

    private String uploadToQiniu(MultipartFile file) throws IOException {
        String accessKey = System.getenv("QINIU_ACCESS_KEY");
        String secretKey = System.getenv("QINIU_SECRET_KEY");
        String bucket = System.getenv("QINIU_BUCKET");
        String domain = System.getenv("QINIU_DOMAIN");

        if (accessKey == null || secretKey == null || bucket == null) {
            throw new BadRequestException("七牛云配置不完整，请检查环境变量 QINIU_ACCESS_KEY, QINIU_SECRET_KEY, QINIU_BUCKET");
        }

        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String ext = getExtension(file.getOriginalFilename());
        String key = dateDir + "/" + UUID.randomUUID().toString().replace("-", "") + "." + ext;

        try {
            HttpClient client = HttpClient.newHttpClient();

            String uploadToken = generateQiniuToken(accessKey, secretKey, bucket);

            String uploadUrl = "https://upload.qiniup.com/putb64/-1/key/" + java.net.URLEncoder.encode(key, java.nio.charset.StandardCharsets.UTF_8);

            String base64Content = java.util.Base64.getEncoder().encodeToString(file.getBytes());

            String bodyJson = "{\"token\":\"" + uploadToken + "\",\"key\":\"" + key + "\",\"file\":\"" + base64Content + "\"}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uploadUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "UpToken " + uploadToken)
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IOException("七牛云上传失败: HTTP " + response.statusCode() + " - " + response.body());
            }

            String url = (domain != null ? domain : "http://" + bucket + ".qiniudns.com") + "/" + key;
            log.info("文件已上传至七牛云: {}", url);
            return url;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("七牛云上传被中断", e);
        }
    }

    private String generateQiniuToken(String accessKey, String secretKey, String bucket) {
        try {
            String policy = "{\"scope\":\"" + bucket + "\",\"deadline\":" + (System.currentTimeMillis() / 1000 + 3600) + "}";
            String encodedPolicy = java.util.Base64.getEncoder().encodeToString(policy.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
            mac.init(new javax.crypto.spec.SecretKeySpec(secretKey.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA1"));
            byte[] encodedMac = mac.doFinal(encodedPolicy.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String encodedMacStr = java.util.Base64.getEncoder().encodeToString(encodedMac);
            return accessKey + ":" + encodedMacStr + ":" + encodedPolicy;
        } catch (Exception e) {
            throw new RuntimeException("生成七牛云上传Token失败", e);
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return "jpg";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1).toLowerCase() : "jpg";
    }
}

package com.contenthub.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class EmailCodeService {

    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    private final JavaMailSender mailSender;
    private final Map<String, CodeEntry> codeStore = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    private static final int CODE_LENGTH = 6;
    private static final long CODE_TTL_MS = 5 * 60 * 1000L;

    public EmailCodeService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public String generateCode(String email) {
        String code = String.format("%0" + CODE_LENGTH + "d", random.nextInt((int) Math.pow(10, CODE_LENGTH)));
        codeStore.put(email, new CodeEntry(code, System.currentTimeMillis() + CODE_TTL_MS));
        return code;
    }

    public boolean verifyCode(String email, String code) {
        CodeEntry entry = codeStore.get(email);
        if (entry == null) {
            return false;
        }
        if (System.currentTimeMillis() > entry.expiresAt) {
            codeStore.remove(email);
            return false;
        }
        if (entry.code.equals(code)) {
            codeStore.remove(email);
            return true;
        }
        return false;
    }

    public void sendCode(String email, String type) {
        String code = generateCode(email);

        if (!emailEnabled || mailSender == null) {
            log.info("邮件服务未启用，验证码（{}）: {}", type, code);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("ContentHub 验证码");
            message.setText("您的验证码是: " + code + "，5分钟内有效。");
            mailSender.send(message);
            log.info("验证码已发送到: {}", email);
        } catch (Exception e) {
            log.error("邮件发送失败: {}", e.getMessage());
        }
    }

    private record CodeEntry(String code, long expiresAt) {}
}

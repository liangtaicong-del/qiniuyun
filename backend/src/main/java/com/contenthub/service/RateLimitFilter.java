package com.contenthub.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Component
@Order(1)
public class RateLimitFilter implements Filter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> globalBuckets = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "rate-limit-cleanup");
        t.setDaemon(true);
        return t;
    });

    @Value("${app.ratelimit.requests-per-minute:60}")
    private int requestsPerMinute;

    @Value("${app.ratelimit.burst-capacity:10}")
    private int burstCapacity;

    @Value("${app.ratelimit.global-requests-per-minute:300}")
    private int globalRequestsPerMinute;

    private static final long CLEANUP_INTERVAL_MS = 5 * 60 * 1000L;
    private static final long BUCKET_IDLE_TTL_MS = 30 * 60 * 1000L;
    private final Map<String, Long> lastAccess = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        cleanupExecutor.scheduleAtFixedRate(this::evictIdleBuckets, CLEANUP_INTERVAL_MS, CLEANUP_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void destroy() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void evictIdleBuckets() {
        long now = System.currentTimeMillis();
        lastAccess.entrySet().removeIf(entry -> {
            if (now - entry.getValue() > BUCKET_IDLE_TTL_MS) {
                buckets.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.classic(requestsPerMinute,
                Refill.greedy(requestsPerMinute, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createGlobalBucket() {
        Bandwidth limit = Bandwidth.classic(globalRequestsPerMinute,
                Refill.greedy(globalRequestsPerMinute, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String path = request.getRequestURI();

        if (path.startsWith("/api/auth/") || path.startsWith("/h2-console")
                || path.startsWith("/actuator") || path.startsWith("/swagger")
                || path.startsWith("/v3/api-docs") || path.equals("/error")) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        lastAccess.put(clientIp, System.currentTimeMillis());
        Bucket perIpBucket = buckets.computeIfAbsent(clientIp, k -> createBucket());

        if (perIpBucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"请求过于频繁，请稍后再试\",\"retryAfter\":60}");
            response.setHeader("Retry-After", "60");
            response.setHeader("X-RateLimit-Limit", String.valueOf(requestsPerMinute));
        }
    }
}

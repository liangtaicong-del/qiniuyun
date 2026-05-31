package com.contenthub.service;

import com.contenthub.common.Constants;
import com.contenthub.dto.PublishRequest;
import com.contenthub.dto.PublishTaskResponse;
import com.contenthub.entity.Article;
import com.contenthub.entity.PlatformAccount;
import com.contenthub.entity.PublishTask;
import com.contenthub.entity.User;
import com.contenthub.exception.BadRequestException;
import com.contenthub.exception.ResourceNotFoundException;
import com.contenthub.platform.PlatformPublisher;
import com.contenthub.platform.PlatformPublisherRegistry;
import com.contenthub.platform.PublishResult;
import com.contenthub.repository.ArticleRepository;
import com.contenthub.repository.PlatformAccountRepository;
import com.contenthub.repository.PublishTaskRepository;
import com.contenthub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublishService {

    private final PublishTaskRepository publishTaskRepository;
    private final ArticleRepository articleRepository;
    private final PlatformAccountRepository platformAccountRepository;
    private final UserRepository userRepository;
    private final PlatformPublisherRegistry publisherRegistry;

    public Page<PublishTaskResponse> getTasks(Long userId, int page, int size, String status) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PublishTask> tasks;
        if (status != null && !status.isBlank()) {
            tasks = publishTaskRepository.findByUserIdAndStatus(userId,
                    PublishTask.TaskStatus.valueOf(status.toUpperCase()), pageRequest);
        } else {
            tasks = publishTaskRepository.findByUserId(userId, pageRequest);
        }
        return tasks.map(this::toResponse);
    }

    @Transactional
    public List<PublishTaskResponse> publishArticle(PublishRequest request, Long userId) {
        Article article = articleRepository.findById(request.getArticleId())
                .filter(a -> a.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("文章不存在"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        List<PublishTaskResponse> results = new ArrayList<>();

        for (String platformStr : request.getPlatforms()) {
            Constants.Platform platform;
            try {
                platform = Constants.Platform.valueOf(platformStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("未知平台: {}", platformStr);
                continue;
            }

            PlatformAccount account = platformAccountRepository.findByUserIdAndPlatform(userId, platform).orElse(null);
            String platformName = account != null ? account.getPlatform().getName() : platform.getName();

            LocalDateTime scheduledAt = null;
            if (request.getScheduledAt() != null) {
                scheduledAt = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(request.getScheduledAt()), ZoneId.systemDefault());
            }

            PublishTask task = PublishTask.builder()
                    .article(article)
                    .platform(platform)
                    .platformName(platformName)
                    .user(user)
                    .status(PublishTask.TaskStatus.PROCESSING)
                    .scheduledAt(scheduledAt)
                    .build();

            task = publishTaskRepository.save(task);

            if (scheduledAt != null) {
                task.setStatus(PublishTask.TaskStatus.PENDING);
                task.setErrorMsg("定时发布任务已创建，将在指定时间执行");
                task = publishTaskRepository.save(task);
            } else {
                PublishResult result = executePublish(article, platform, account);
                if (result.success()) {
                    task.setStatus(PublishTask.TaskStatus.SUCCESS);
                    task.setPlatformUrl(result.platformUrl());
                    task.setPublishedAt(LocalDateTime.now());
                    task.setErrorMsg(null);
                } else {
                    task.setStatus(PublishTask.TaskStatus.FAILED);
                    task.setErrorMsg(result.errorMessage());
                }
                task = publishTaskRepository.save(task);
            }

            results.add(toResponse(task));
        }

        return results;
    }

    private PublishResult executePublish(Article article, Constants.Platform platform, PlatformAccount account) {
        PlatformPublisher publisher = publisherRegistry.getPublisher(platform);

        if (publisher == null) {
            return PublishResult.failure("不支持的平台: " + platform.getName());
        }

        String accessToken = account != null ? account.getAccessToken() : null;

        if (accessToken == null || accessToken.startsWith("mock-")) {
            return doMockPublish(platform, article);
        }

        try {
            return publisher.publish(article, accessToken);
        } catch (Exception e) {
            log.error("平台 {} 发布异常: {}", platform, e.getMessage(), e);
            return PublishResult.failure("发布异常: " + e.getMessage());
        }
    }

    private PublishResult doMockPublish(Constants.Platform platform, Article article) {
        String mockUrl = "https://mock." + platform.name().toLowerCase() + ".com/article/" + article.getId();
        log.info("平台 {} 使用模拟模式发布文章: {}", platform.getName(), article.getTitle());
        return PublishResult.success(mockUrl);
    }

    @Transactional
    public void cancelTask(Long taskId, Long userId) {
        PublishTask task = publishTaskRepository.findById(taskId)
                .filter(t -> t.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("任务不存在"));

        if (task.getStatus() != PublishTask.TaskStatus.PENDING) {
            throw new BadRequestException("只能取消待发布的任务");
        }

        task.setStatus(PublishTask.TaskStatus.CANCELLED);
        publishTaskRepository.save(task);
    }

    @Transactional
    public PublishTaskResponse retryTask(Long taskId, Long userId) {
        PublishTask task = publishTaskRepository.findById(taskId)
                .filter(t -> t.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("任务不存在"));

        if (task.getStatus() != PublishTask.TaskStatus.FAILED) {
            throw new BadRequestException("只能重试失败的任务");
        }

        Long userIdVal = task.getUser().getId();
        Article article = task.getArticle();
        PlatformAccount account = platformAccountRepository
                .findByUserIdAndPlatform(userIdVal, task.getPlatform()).orElse(null);

        task.setStatus(PublishTask.TaskStatus.PROCESSING);
        task.setErrorMsg(null);
        publishTaskRepository.save(task);

        PublishResult result = executePublish(article, task.getPlatform(), account);
        if (result.success()) {
            task.setStatus(PublishTask.TaskStatus.SUCCESS);
            task.setPlatformUrl(result.platformUrl());
            task.setPublishedAt(LocalDateTime.now());
            task.setErrorMsg(null);
        } else {
            task.setStatus(PublishTask.TaskStatus.FAILED);
            task.setErrorMsg(result.errorMessage());
        }

        task = publishTaskRepository.save(task);
        return toResponse(task);
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void processScheduledTasks() {
        List<PublishTask> tasks = publishTaskRepository.findByStatusAndScheduledAtBefore(
                PublishTask.TaskStatus.PENDING, LocalDateTime.now());

        for (PublishTask task : tasks) {
            Long userId = task.getUser().getId();
            Article article = task.getArticle();
            PlatformAccount account = platformAccountRepository
                    .findByUserIdAndPlatform(userId, task.getPlatform()).orElse(null);

            task.setStatus(PublishTask.TaskStatus.PROCESSING);
            publishTaskRepository.save(task);

            PublishResult result = executePublish(article, task.getPlatform(), account);
            if (result.success()) {
                task.setStatus(PublishTask.TaskStatus.SUCCESS);
                task.setPlatformUrl(result.platformUrl());
                task.setPublishedAt(LocalDateTime.now());
                task.setErrorMsg(null);
            } else {
                task.setStatus(PublishTask.TaskStatus.FAILED);
                task.setErrorMsg(result.errorMessage());
            }
            publishTaskRepository.save(task);
        }
    }

    private PublishTaskResponse toResponse(PublishTask task) {
        return PublishTaskResponse.builder()
                .id(task.getId())
                .articleId(task.getArticle().getId())
                .articleTitle(task.getArticle().getTitle())
                .platform(task.getPlatform().name())
                .platformName(task.getPlatformName())
                .status(task.getStatus().name())
                .statusName(getStatusName(task.getStatus()))
                .scheduledAt(task.getScheduledAt())
                .publishedAt(task.getPublishedAt())
                .platformUrl(task.getPlatformUrl())
                .errorMsg(task.getErrorMsg())
                .build();
    }

    private String getStatusName(PublishTask.TaskStatus status) {
        return switch (status) {
            case PENDING -> "待发布";
            case PROCESSING -> "发布中";
            case SUCCESS -> "已发布";
            case FAILED -> "发布失败";
            case CANCELLED -> "已取消";
        };
    }
}

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
import com.contenthub.repository.ArticleRepository;
import com.contenthub.repository.PlatformAccountRepository;
import com.contenthub.repository.PublishTaskRepository;
import com.contenthub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class PublishService {

    private final PublishTaskRepository publishTaskRepository;
    private final ArticleRepository articleRepository;
    private final PlatformAccountRepository platformAccountRepository;
    private final UserRepository userRepository;

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
                    .status(scheduledAt != null ? PublishTask.TaskStatus.PENDING : PublishTask.TaskStatus.SUCCESS)
                    .scheduledAt(scheduledAt)
                    .platformUrl("https://mock." + platformStr.toLowerCase() + ".com/article/" + article.getId())
                    .publishedAt(scheduledAt == null ? LocalDateTime.now() : null)
                    .errorMsg(scheduledAt != null ? null : "模拟发布成功")
                    .build();

            task = publishTaskRepository.save(task);
            results.add(toResponse(task));
        }

        return results;
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

        task.setStatus(PublishTask.TaskStatus.PENDING);
        task.setScheduledAt(LocalDateTime.now());
        task.setPlatformUrl(null);
        task.setErrorMsg(null);
        task = publishTaskRepository.save(task);
        return toResponse(task);
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void processScheduledTasks() {
        List<PublishTask> tasks = publishTaskRepository.findByStatusAndScheduledAtBefore(
                PublishTask.TaskStatus.PENDING, LocalDateTime.now());

        for (PublishTask task : tasks) {
            task.setStatus(PublishTask.TaskStatus.SUCCESS);
            task.setPublishedAt(LocalDateTime.now());
            task.setPlatformUrl("https://mock." + task.getPlatform().name().toLowerCase() + ".com/article/" + task.getArticle().getId());
            task.setErrorMsg("定时发布模拟成功");
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

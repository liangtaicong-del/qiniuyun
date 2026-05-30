package com.contenthub.service;

import com.contenthub.common.Constants;
import com.contenthub.dto.StatsResponse;
import com.contenthub.entity.Article;
import com.contenthub.entity.PublishTask;
import com.contenthub.repository.ArticleRepository;
import com.contenthub.repository.PlatformAccountRepository;
import com.contenthub.repository.PublishTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final ArticleRepository articleRepository;
    private final PublishTaskRepository publishTaskRepository;
    private final PlatformAccountRepository platformAccountRepository;

    public StatsResponse getOverview(Long userId) {
        long totalArticles = articleRepository.countByUserId(userId);
        long publishedArticles = articleRepository.countByUserIdAndStatus(userId, Article.ArticleStatus.PUBLISHED);
        long draftArticles = articleRepository.countByUserIdAndStatus(userId, Article.ArticleStatus.DRAFT);
        long successTasks = publishTaskRepository.countByUserIdAndStatus(userId, PublishTask.TaskStatus.SUCCESS);
        long failedTasks = publishTaskRepository.countByUserIdAndStatus(userId, PublishTask.TaskStatus.FAILED);
        long totalTasks = publishTaskRepository.countByUserId(userId);

        Map<String, Integer> draftTrend = buildDailyTrend(userId, Article.ArticleStatus.DRAFT);
        Map<String, Integer> publishedTrend = buildDailyTrend(userId, Article.ArticleStatus.PUBLISHED);

        StatsResponse.OverviewStats overview = StatsResponse.OverviewStats.builder()
                .totalArticles((int) totalArticles)
                .publishedArticles((int) publishedArticles)
                .draftArticles((int) draftArticles)
                .successTasks((int) successTasks)
                .failedTasks((int) failedTasks)
                .totalTasks((int) totalTasks)
                .build();

        List<StatsResponse.PlatformStats> platformStats = buildPlatformStats(userId);

        return StatsResponse.builder()
                .overview(overview)
                .draftTrend(draftTrend)
                .publishedTrend(publishedTrend)
                .platforms(platformStats)
                .build();
    }

    private Map<String, Integer> buildDailyTrend(Long userId, Article.ArticleStatus status) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        Map<String, Integer> result = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            result.put(date.format(fmt), 0);
        }

        List<Article> articles = articleRepository.findByUserIdAndStatus(userId, status,
                org.springframework.data.domain.Pageable.unpaged()).getContent();

        for (Article article : articles) {
            if (article.getCreatedAt() != null) {
                String dateKey = article.getCreatedAt().toLocalDate().format(fmt);
                if (result.containsKey(dateKey)) {
                    result.put(dateKey, result.get(dateKey) + 1);
                }
            }
        }

        return result;
    }

    private List<StatsResponse.PlatformStats> buildPlatformStats(Long userId) {
        List<StatsResponse.PlatformStats> result = new ArrayList<>();
        for (Constants.Platform platform : Constants.Platform.values()) {
            long total = publishTaskRepository.countByUserIdAndPlatformAndStatus(userId, platform, null);
            long success = publishTaskRepository.countByUserIdAndPlatformAndStatus(userId, platform, PublishTask.TaskStatus.SUCCESS);
            long failed = publishTaskRepository.countByUserIdAndPlatformAndStatus(userId, platform, PublishTask.TaskStatus.FAILED);
            if (total > 0 || platformAccountRepository.existsByUserIdAndPlatform(userId, platform)) {
                result.add(StatsResponse.PlatformStats.builder()
                        .platform(platform.name())
                        .platformName(platform.getName())
                        .totalCount((int) total)
                        .successCount((int) success)
                        .failedCount((int) failed)
                        .build());
            }
        }
        return result;
    }
}

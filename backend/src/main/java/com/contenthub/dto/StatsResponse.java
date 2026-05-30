package com.contenthub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsResponse {
    private OverviewStats overview;
    private Map<String, Integer> draftTrend;
    private Map<String, Integer> publishedTrend;
    private java.util.List<PlatformStats> platforms;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverviewStats {
        private Integer totalArticles;
        private Integer publishedArticles;
        private Integer draftArticles;
        private Integer successTasks;
        private Integer failedTasks;
        private Integer totalTasks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlatformStats {
        private String platform;
        private String platformName;
        private Integer totalCount;
        private Integer successCount;
        private Integer failedCount;
    }
}

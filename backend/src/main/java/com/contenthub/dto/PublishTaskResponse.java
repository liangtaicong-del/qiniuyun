package com.contenthub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishTaskResponse {
    private Long id;
    private Long articleId;
    private String articleTitle;
    private String platform;
    private String platformName;
    private String status;
    private String statusName;
    private LocalDateTime scheduledAt;
    private LocalDateTime publishedAt;
    private String platformUrl;
    private String errorMsg;
}

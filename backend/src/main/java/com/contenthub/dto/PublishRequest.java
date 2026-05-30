package com.contenthub.dto;

import lombok.Data;

@Data
public class PublishRequest {
    private Long articleId;
    private java.util.List<String> platforms;
    private Long scheduledAt;
}

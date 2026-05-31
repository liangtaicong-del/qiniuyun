package com.contenthub.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublishRequest {
    private Long articleId;
    private java.util.List<String> platforms;
    private Long scheduledAt;
}

package com.contenthub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingsResponse {
    private Long id;
    private String defaultPlatform;
    private Boolean autoPublish;
    private Integer publishInterval;
    private Boolean emailNotification;
    private Boolean pushNotification;
    private Boolean publishAutoRetry;
    private String theme;
    private String language;
}

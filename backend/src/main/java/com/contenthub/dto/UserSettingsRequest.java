package com.contenthub.dto;

import lombok.Data;

@Data
public class UserSettingsRequest {
    private String defaultPlatform;
    private Boolean autoPublish;
    private Integer publishInterval;
    private Boolean emailNotification;
    private Boolean pushNotification;
    private Boolean publishAutoRetry;
    private String theme;
    private String language;
}

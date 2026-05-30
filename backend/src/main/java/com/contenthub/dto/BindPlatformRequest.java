package com.contenthub.dto;

import lombok.Data;

@Data
public class BindPlatformRequest {
    private String platform;
    private String authCode;
}

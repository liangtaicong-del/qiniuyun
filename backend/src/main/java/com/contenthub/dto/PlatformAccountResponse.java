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
public class PlatformAccountResponse {
    private Long id;
    private String platform;
    private String platformName;
    private String accountName;
    private LocalDateTime bindTime;
}

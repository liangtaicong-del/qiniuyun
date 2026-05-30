package com.contenthub.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String defaultPlatform;

    @Builder.Default
    private Boolean autoPublish = false;

    @Builder.Default
    private Integer publishInterval = 10;

    @Builder.Default
    private Boolean emailNotification = true;

    @Builder.Default
    private Boolean pushNotification = true;

    @Builder.Default
    private Boolean publishAutoRetry = true;

    @Builder.Default
    private String theme = "light";

    @Builder.Default
    private String language = "zh-CN";
}

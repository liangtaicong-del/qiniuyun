package com.contenthub.entity;

import com.contenthub.common.Constants;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "platform_accounts", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "platform"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Constants.Platform platform;

    @Column(length = 200)
    private String accountName;

    @Column(length = 1000)
    private String accessToken;

    @Column(length = 1000)
    private String refreshToken;

    private LocalDateTime tokenExpiredAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime bindTime;
}

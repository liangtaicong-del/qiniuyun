package com.contenthub.repository;

import com.contenthub.entity.PlatformAccount;
import com.contenthub.common.Constants;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlatformAccountRepository extends JpaRepository<PlatformAccount, Long> {
    List<PlatformAccount> findByUserId(Long userId);
    Optional<PlatformAccount> findByUserIdAndPlatform(Long userId, Constants.Platform platform);
    boolean existsByUserIdAndPlatform(Long userId, Constants.Platform platform);
    void deleteByUserIdAndPlatform(Long userId, Constants.Platform platform);
}

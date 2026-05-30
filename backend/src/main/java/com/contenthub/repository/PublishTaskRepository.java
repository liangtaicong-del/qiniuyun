package com.contenthub.repository;

import com.contenthub.common.Constants;
import com.contenthub.entity.PublishTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PublishTaskRepository extends JpaRepository<PublishTask, Long> {
    Page<PublishTask> findByUserId(Long userId, Pageable pageable);

    Page<PublishTask> findByUserIdAndStatus(Long userId, PublishTask.TaskStatus status, Pageable pageable);

    List<PublishTask> findByStatusAndScheduledAtBefore(PublishTask.TaskStatus status, java.time.LocalDateTime time);

    long countByUserIdAndStatus(Long userId, PublishTask.TaskStatus status);

    long countByUserId(Long userId);

    @Query("SELECT COUNT(t) FROM PublishTask t WHERE t.user.id = :userId AND t.platform = :platform" +
           " AND (:status IS NULL OR t.status = :status)")
    long countByUserIdAndPlatformAndStatus(
            @Param("userId") Long userId,
            @Param("platform") Constants.Platform platform,
            @Param("status") PublishTask.TaskStatus status);
}

package com.contenthub.repository;

import com.contenthub.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    Page<Article> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT a FROM Article a WHERE a.user.id = :userId AND a.status = :status")
    Page<Article> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Article.ArticleStatus status, Pageable pageable);

    @Query("SELECT a FROM Article a WHERE a.user.id = :userId AND a.title LIKE %:keyword%")
    Page<Article> findByUserIdAndTitleContaining(@Param("userId") Long userId, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT a FROM Article a WHERE a.user.id = :userId AND a.title LIKE %:keyword% AND a.status = :status")
    Page<Article> findByUserIdAndTitleContainingAndStatus(@Param("userId") Long userId, @Param("keyword") String keyword, @Param("status") Article.ArticleStatus status, Pageable pageable);

    List<Article> findTopNByUserIdOrderByCreatedAtDesc(Long userId, org.springframework.data.domain.Pageable pageable);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, Article.ArticleStatus status);
}

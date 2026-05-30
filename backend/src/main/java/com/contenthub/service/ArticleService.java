package com.contenthub.service;

import com.contenthub.dto.*;
import com.contenthub.entity.Article;
import com.contenthub.entity.User;
import com.contenthub.exception.ResourceNotFoundException;
import com.contenthub.repository.ArticleRepository;
import com.contenthub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;

    public Page<ArticleResponse> getArticles(Long userId, String keyword, String status, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Article> articles;

        if (keyword != null && !keyword.isBlank() && status != null && !status.isBlank()) {
            articles = articleRepository.findByUserIdAndTitleContainingAndStatus(userId, keyword, Article.ArticleStatus.valueOf(status), pageRequest);
        } else if (keyword != null && !keyword.isBlank()) {
            articles = articleRepository.findByUserIdAndTitleContaining(userId, keyword, pageRequest);
        } else if (status != null && !status.isBlank()) {
            articles = articleRepository.findByUserIdAndStatus(userId, Article.ArticleStatus.valueOf(status), pageRequest);
        } else {
            articles = articleRepository.findByUserId(userId, pageRequest);
        }

        return articles.map(this::toResponse);
    }

    public ArticleResponse getArticle(Long id, Long userId) {
        Article article = articleRepository.findById(id)
                .filter(a -> a.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("文章不存在"));
        return toResponse(article);
    }

    @Transactional
    public ArticleResponse createArticle(ArticleRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        Article article = Article.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .summary(request.getSummary())
                .coverImage(request.getCoverImage())
                .status(request.getStatus() != null ? Article.ArticleStatus.valueOf(request.getStatus()) : Article.ArticleStatus.DRAFT)
                .tags(request.getTags() != null ? request.getTags() : List.of())
                .user(user)
                .viewCount(0)
                .build();

        article = articleRepository.save(article);
        return toResponse(article);
    }

    @Transactional
    public ArticleResponse updateArticle(Long id, ArticleRequest request, Long userId) {
        Article article = articleRepository.findById(id)
                .filter(a -> a.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("文章不存在"));

        if (request.getTitle() != null) article.setTitle(request.getTitle());
        if (request.getContent() != null) article.setContent(request.getContent());
        if (request.getSummary() != null) article.setSummary(request.getSummary());
        if (request.getCoverImage() != null) article.setCoverImage(request.getCoverImage());
        if (request.getStatus() != null) article.setStatus(Article.ArticleStatus.valueOf(request.getStatus()));
        if (request.getTags() != null) article.setTags(request.getTags());

        article = articleRepository.save(article);
        return toResponse(article);
    }

    @Transactional
    public void deleteArticle(Long id, Long userId) {
        Article article = articleRepository.findById(id)
                .filter(a -> a.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("文章不存在"));
        articleRepository.delete(article);
    }

    public List<ArticleResponse> getRecentArticles(Long userId, int limit) {
        return articleRepository.findTopNByUserIdOrderByCreatedAtDesc(userId,
                org.springframework.data.domain.PageRequest.of(0, limit))
                .stream().map(this::toResponse).toList();
    }

    private ArticleResponse toResponse(Article article) {
        return ArticleResponse.builder()
                .id(article.getId())
                .title(article.getTitle())
                .content(article.getContent())
                .summary(article.getSummary())
                .coverImage(article.getCoverImage())
                .status(article.getStatus().name())
                .tags(article.getTags())
                .viewCount(article.getViewCount())
                .createdAt(article.getCreatedAt())
                .updatedAt(article.getUpdatedAt())
                .build();
    }
}

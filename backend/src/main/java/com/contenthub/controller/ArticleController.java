package com.contenthub.controller;

import com.contenthub.dto.*;
import com.contenthub.service.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ArticleResponse>>> getArticles(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        Long userId = (Long) auth.getPrincipal();
        Page<ArticleResponse> articles = articleService.getArticles(userId, keyword, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(articles));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ArticleResponse>> getArticle(Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        ArticleResponse article = articleService.getArticle(id, userId);
        return ResponseEntity.ok(ApiResponse.success(article));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ArticleResponse>> createArticle(
            Authentication auth,
            @Valid @RequestBody ArticleRequest request) {
        Long userId = (Long) auth.getPrincipal();
        ArticleResponse article = articleService.createArticle(request, userId);
        return ResponseEntity.ok(ApiResponse.success(article));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ArticleResponse>> updateArticle(
            Authentication auth,
            @PathVariable Long id,
            @RequestBody ArticleRequest request) {
        Long userId = (Long) auth.getPrincipal();
        ArticleResponse article = articleService.updateArticle(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success(article));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteArticle(Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        articleService.deleteArticle(id, userId);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<ArticleResponse>>> getRecentArticles(
            Authentication auth,
            @RequestParam(defaultValue = "5") int limit) {
        Long userId = (Long) auth.getPrincipal();
        List<ArticleResponse> articles = articleService.getRecentArticles(userId, limit);
        return ResponseEntity.ok(ApiResponse.success(articles));
    }
}

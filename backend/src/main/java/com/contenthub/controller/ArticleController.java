package com.contenthub.controller;

import com.contenthub.dto.*;
import com.contenthub.service.ArticleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ArticleResponse>>> getArticles(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        Long userId = (Long) request.getAttribute("userId");
        Page<ArticleResponse> articles = articleService.getArticles(userId, keyword, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(articles));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ArticleResponse>> getArticle(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        ArticleResponse article = articleService.getArticle(id, userId);
        return ResponseEntity.ok(ApiResponse.success(article));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ArticleResponse>> createArticle(
            HttpServletRequest request,
            @Valid @RequestBody ArticleRequest req) {
        Long userId = (Long) request.getAttribute("userId");
        ArticleResponse article = articleService.createArticle(req, userId);
        return ResponseEntity.ok(ApiResponse.success(article));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ArticleResponse>> updateArticle(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestBody ArticleRequest req) {
        Long userId = (Long) request.getAttribute("userId");
        ArticleResponse article = articleService.updateArticle(id, req, userId);
        return ResponseEntity.ok(ApiResponse.success(article));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteArticle(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        articleService.deleteArticle(id, userId);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<ArticleResponse>>> getRecentArticles(
            HttpServletRequest request,
            @RequestParam(defaultValue = "5") int limit) {
        Long userId = (Long) request.getAttribute("userId");
        List<ArticleResponse> articles = articleService.getRecentArticles(userId, limit);
        return ResponseEntity.ok(ApiResponse.success(articles));
    }
}

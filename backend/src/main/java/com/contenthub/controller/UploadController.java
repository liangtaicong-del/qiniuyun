package com.contenthub.controller;

import com.contenthub.dto.ApiResponse;
import com.contenthub.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final FileStorageService fileStorageService;

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> upload(
            @RequestParam("file") MultipartFile file) throws IOException {
        String url = fileStorageService.upload(file);
        return ResponseEntity.ok(ApiResponse.success(Map.of("url", url)));
    }
}

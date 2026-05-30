package com.contenthub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ArticleRequest {
    @NotBlank(message = "标题不能为空")
    @Size(max = 500, message = "标题不能超过500个字符")
    private String title;

    private String content;

    @Size(max = 1000, message = "摘要不能超过1000个字符")
    private String summary;

    private String coverImage;

    private String status;

    private java.util.List<String> tags;
}

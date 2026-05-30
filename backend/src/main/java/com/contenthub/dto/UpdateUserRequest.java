package com.contenthub.dto;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String username;
    private String bio;
    private String avatar;
}

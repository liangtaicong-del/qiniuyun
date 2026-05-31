package com.contenthub.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @GetMapping("/headers")
    public String debugHeaders(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestHeader(value = "Origin", required = false) String origin) {
        return "auth=" + auth + ", origin=" + origin;
    }
}

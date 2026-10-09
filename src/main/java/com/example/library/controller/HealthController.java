package com.example.library.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Public endpoints used by hosting platforms (Render / Railway) as a health check. */
@RestController
public class HealthController {

    @GetMapping("/")
    public Map<String, String> root() {
        return Map.of("name", "Library Management API", "status", "UP");
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}

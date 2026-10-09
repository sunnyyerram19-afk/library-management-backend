package com.example.library.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ApiError(int status, String message, Map<String, String> errors, LocalDateTime timestamp) {

    public static ApiError of(int status, String message) {
        return new ApiError(status, message, null, LocalDateTime.now());
    }
}

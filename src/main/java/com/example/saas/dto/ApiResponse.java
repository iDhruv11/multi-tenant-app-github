package com.example.saas.dto;

import java.time.Instant;

public record ApiResponse<T>(
    T data,
    Instant timestamp,
    String requestId) {
}

package com.learn.projeto_learn.exception;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
        int status,
        String error,
        String message,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message) {}

    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(status, error, message, LocalDateTime.now(), List.of());
    }

    public static ErrorResponse of(int status, String error, String message, List<FieldError> fieldErrors) {
        return new ErrorResponse(status, error, message, LocalDateTime.now(), fieldErrors);
    }
}

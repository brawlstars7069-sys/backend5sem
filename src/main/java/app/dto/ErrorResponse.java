package app.dto;

public record ErrorResponse(
        int status,
        String error,
        String message
) {}

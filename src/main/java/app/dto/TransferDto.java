package app.dto;

import jakarta.validation.constraints.NotNull;

public record TransferDto(
        @NotNull(message = "Параметр targetComputerId обязателен")
        Long targetComputerId,

        @NotNull(message = "Параметр playerId обязателен")
        Long playerId
) {}

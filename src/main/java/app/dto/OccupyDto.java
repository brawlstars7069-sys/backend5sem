package app.dto;

import jakarta.validation.constraints.NotNull;

public record OccupyDto(
        @NotNull(message = "Параметр playerId обязателен")
        Long playerId
) {}

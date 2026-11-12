package app.dto;

import app.domain.Model.PlayerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PlayerDto(
        @NotBlank(message = "Никнейм игрока обязателен")
        String nickname,

        @NotBlank(message = "Телефон игрока обязателен")
        String phone,

        @NotNull(message = "Тип игрока обязателен (REGULAR, VIP)")
        PlayerType type
) {}

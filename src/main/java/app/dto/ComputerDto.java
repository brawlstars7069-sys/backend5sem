package app.dto;

import app.domain.Model.ComputerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ComputerDto(
        @NotBlank(message = "Имя компьютера обязательно")
        String name,

        @NotNull(message = "Тип компьютера обязателен (STANDARD, VIP)")
        ComputerType type,

        @NotNull(message = "Почасовой тариф обязателен")
        @Positive(message = "Почасовой тариф должен быть > 0")
        Double hourlyRate
) {}

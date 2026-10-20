package ru.prokin.kitchen.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkAdRequest {
    @NotBlank(message = "Доменный логин AD обязателен")
    private String adUsername;

    @NotBlank(message = "Пароль учетной записи AD обязателен")
    private String adPassword;
}

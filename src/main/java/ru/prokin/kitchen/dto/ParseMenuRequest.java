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
public class ParseMenuRequest {
    @NotBlank(message = "URL для парсинга меню обязателен")
    private String url;
}

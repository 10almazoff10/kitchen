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
public class RestaurantRequest {
    @NotBlank(message = "Название ресторана обязательно")
    private String name;

    @NotBlank(message = "Ссылка на сайт ресторана обязательна")
    private String websiteUrl;
}

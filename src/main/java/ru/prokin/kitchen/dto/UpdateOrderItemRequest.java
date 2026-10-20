package ru.prokin.kitchen.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderItemRequest {
    @NotBlank(message = "Описание блюда обязательно")
    private String itemDescription;

    @NotNull(message = "Цена обязательна")
    @DecimalMin(value = "0.0", inclusive = true, message = "Цена не может быть отрицательной")
    private BigDecimal price;
}

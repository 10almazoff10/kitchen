package ru.prokin.kitchen.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    @NotNull(message = "ID ресторана обязательно")
    private Long restaurantId;

    @NotNull(message = "Дедлайн заказа обязателен")
    private LocalDateTime deadlineTime;

    private String paymentData;
    private String comment;
}

package ru.prokin.kitchen.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.prokin.kitchen.entity.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private RestaurantResponse restaurant;
    private LocalDateTime deadlineTime;
    private boolean isClosed;
    private boolean isAcceptingOrders;
    private String paymentData;
    private String comment;
    private UserDto createdBy;
    private BigDecimal totalAmount;
    private int itemsCount;

    @com.fasterxml.jackson.annotation.JsonProperty("closed")
    public boolean isClosed() {
        return isClosed;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("isClosed")
    public boolean getIsClosed() {
        return isClosed;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("acceptingOrders")
    public boolean isAcceptingOrders() {
        return isAcceptingOrders;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("isAcceptingOrders")
    public boolean getIsAcceptingOrders() {
        return isAcceptingOrders;
    }

    public static OrderResponse fromEntity(Order order, BigDecimal totalAmount, int itemsCount) {
        if (order == null) return null;
        return OrderResponse.builder()
                .id(order.getId())
                .restaurant(RestaurantResponse.fromEntity(order.getRestaurant()))
                .deadlineTime(order.getDeadlineTime())
                .isClosed(order.isClosed())
                .isAcceptingOrders(order.isAcceptingOrders())
                .paymentData(order.getPaymentData())
                .comment(order.getComment())
                .createdBy(UserDto.fromEntity(order.getCreatedBy()))
                .totalAmount(totalAmount != null ? totalAmount : BigDecimal.ZERO)
                .itemsCount(itemsCount)
                .build();
    }
}

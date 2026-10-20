package ru.prokin.kitchen.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.prokin.kitchen.entity.UserOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {
    private Long id;
    private Long orderId;
    private Long userId;
    private String userFullName;
    private String itemDescription;
    private BigDecimal price;
    private BigDecimal deliveryCost;
    private BigDecimal totalCost;
    private boolean isAddedToRestaurantOrder;
    private boolean isPaid;
    private LocalDateTime createdDate;
    private Integer rating;

    @com.fasterxml.jackson.annotation.JsonProperty("addedToRestaurantOrder")
    public boolean isAddedToRestaurantOrder() {
        return isAddedToRestaurantOrder;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("isAddedToRestaurantOrder")
    public boolean getIsAddedToRestaurantOrder() {
        return isAddedToRestaurantOrder;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("paid")
    public boolean isPaid() {
        return isPaid;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("isPaid")
    public boolean getIsPaid() {
        return isPaid;
    }

    public static OrderItemResponse fromEntity(UserOrder item) {
        if (item == null) return null;
        BigDecimal delivery = item.getDeliveryCost() != null ? item.getDeliveryCost() : BigDecimal.ZERO;
        BigDecimal price = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;

        return OrderItemResponse.builder()
                .id(item.getId())
                .orderId(item.getOrder() != null ? item.getOrder().getId() : null)
                .userId(item.getUser() != null ? item.getUser().getId() : null)
                .userFullName(item.getUser() != null ? item.getUser().getFullName() : null)
                .itemDescription(item.getItemDescription())
                .price(price)
                .deliveryCost(delivery)
                .totalCost(price.add(delivery))
                .isAddedToRestaurantOrder(item.isAddedToRestaurantOrder())
                .isPaid(item.isPaid())
                .createdDate(item.getCreatedDate())
                .rating(item.getRating())
                .build();
    }
}

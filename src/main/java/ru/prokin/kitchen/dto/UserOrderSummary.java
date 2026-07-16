package ru.prokin.kitchen.dto;

import ru.prokin.kitchen.entity.UserOrder;

import java.math.BigDecimal;
import java.util.List;

public class UserOrderSummary {
    private Long userId;
    private String fullName;
    private List<UserOrder> items;
    private BigDecimal deliveryCost;
    private boolean allPaid;
    private boolean allAddedToRestaurantOrder;

    public UserOrderSummary(Long userId, String fullName, List<UserOrder> items,
                           BigDecimal deliveryCost,
                           boolean allPaid, boolean allAddedToRestaurantOrder) {
        this.userId = userId;
        this.fullName = fullName;
        this.items = items;
        this.deliveryCost = deliveryCost;
        this.allPaid = allPaid;
        this.allAddedToRestaurantOrder = allAddedToRestaurantOrder;
    }

    // Геттеры
    public Long getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public List<UserOrder> getItems() {
        return items;
    }

    public BigDecimal getDeliveryCost() {
        return deliveryCost;
    }

    public boolean isAllPaid() {
        return allPaid;
    }

    public boolean isAllAddedToRestaurantOrder() {
        return allAddedToRestaurantOrder;
    }
}
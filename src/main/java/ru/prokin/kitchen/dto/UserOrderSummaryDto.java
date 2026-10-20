package ru.prokin.kitchen.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserOrderSummaryDto {
    private Long userId;
    private String userFullName;
    private String avatarUrl;
    private List<OrderItemResponse> items;
    private BigDecimal deliveryCost;
    private BigDecimal totalAmount;
    private boolean allPaid;
    private boolean allAdded;
}

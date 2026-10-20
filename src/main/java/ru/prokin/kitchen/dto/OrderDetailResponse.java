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
public class OrderDetailResponse {
    private OrderResponse order;
    private List<OrderItemResponse> items;
    private List<UserOrderSummaryDto> userSummaries;
    private BigDecimal totalAmount;
    private BigDecimal totalDeliveryCost;
}

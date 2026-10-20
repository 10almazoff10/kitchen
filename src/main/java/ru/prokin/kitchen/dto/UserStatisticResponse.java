package ru.prokin.kitchen.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatisticResponse {
    private BigDecimal totalSpentThisMonth;
    private long totalOrdersThisMonth;
    private BigDecimal totalSpentAllTime;
    private long totalOrdersAllTime;
}

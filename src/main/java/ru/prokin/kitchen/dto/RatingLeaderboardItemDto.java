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
public class RatingLeaderboardItemDto {
    private Long userId;
    private String username;
    private String fullName;
    private String email;
    private String avatarUrl;
    private BigDecimal amount;
    private Long count;
}

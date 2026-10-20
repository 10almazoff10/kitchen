package ru.prokin.kitchen.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingLeaderboardResponse {
    private List<RatingLeaderboardItemDto> topSpentThisMonth;
    private List<RatingLeaderboardItemDto> topOrdersThisMonth;
    private List<RatingLeaderboardItemDto> topSpentAllTime;
    private List<RatingLeaderboardItemDto> topOrdersAllTime;
}

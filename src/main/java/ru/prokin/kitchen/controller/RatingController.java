package ru.prokin.kitchen.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.prokin.kitchen.dto.*;
import ru.prokin.kitchen.entity.User;
import ru.prokin.kitchen.service.RatingService;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @GetMapping("/leaderboards")
    public ResponseEntity<RatingLeaderboardResponse> getLeaderboards() {
        return ResponseEntity.ok(RatingLeaderboardResponse.builder()
                .topSpentThisMonth(mapToLeaderboard(ratingService.getTopSpentThisMonth()))
                .topOrdersThisMonth(mapToLeaderboard(ratingService.getTopOrdersThisMonth()))
                .topSpentAllTime(mapToLeaderboard(ratingService.getTopSpentAllTime()))
                .topOrdersAllTime(mapToLeaderboard(ratingService.getTopOrdersAllTime()))
                .build());
    }

    @GetMapping("/my-stats")
    public ResponseEntity<UserStatisticResponse> getMyStats(@AuthenticationPrincipal User user) {
        RatingService.UserStatistic stat = ratingService.getUserStatistic(user);
        return ResponseEntity.ok(UserStatisticResponse.builder()
                .totalSpentThisMonth(stat.getTotalSpentThisMonth())
                .totalOrdersThisMonth(stat.getTotalOrdersThisMonth())
                .totalSpentAllTime(stat.getTotalSpentAllTime())
                .totalOrdersAllTime(stat.getTotalOrdersAllTime())
                .build());
    }

    private List<RatingLeaderboardItemDto> mapToLeaderboard(List<RatingService.RatingItem> list) {
        return list.stream()
                .map(item -> {
                    BigDecimal spent = null;
                    Long count = null;
                    if (item.getValue() instanceof BigDecimal) {
                        spent = (BigDecimal) item.getValue();
                    } else if (item.getValue() instanceof Number) {
                        count = ((Number) item.getValue()).longValue();
                    }
                    return RatingLeaderboardItemDto.builder()
                            .userId(item.getUser().getId())
                            .username(item.getUser().getUsername())
                            .fullName(item.getUser().getFullName())
                            .email(item.getUser().getEffectiveEmail())
                            .avatarUrl(item.getUser().getAvatarUrl())
                            .amount(spent)
                            .count(count)
                            .build();
                })
                .collect(Collectors.toList());
    }
}

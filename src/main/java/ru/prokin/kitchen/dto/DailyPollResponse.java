package ru.prokin.kitchen.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyPollResponse {
    private Long id;
    private LocalDate pollDate;

    @JsonProperty("isActive")
    private boolean active;

    private int totalVotes;
    private Long myVotedRestaurantId;

    @Builder.Default
    private List<Long> myVotedRestaurantIds = new ArrayList<>();

    @Builder.Default
    private List<PollOptionDto> options = new ArrayList<>();
}

package ru.prokin.kitchen.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PollOptionDto {
    private Long restaurantId;
    private String restaurantName;
    private String restaurantUrl;
    private int votesCount;
    private double percentage;

    @Builder.Default
    private List<UserDto> voters = new ArrayList<>();

    @JsonProperty("isMyVote")
    private boolean isMyVote;
}

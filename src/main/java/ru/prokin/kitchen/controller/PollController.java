package ru.prokin.kitchen.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.prokin.kitchen.dto.DailyPollResponse;
import ru.prokin.kitchen.dto.VoteRequest;
import ru.prokin.kitchen.entity.User;
import ru.prokin.kitchen.service.PollService;

@RestController
@RequestMapping("/api/v1/polls")
@RequiredArgsConstructor
public class PollController {

    private final PollService pollService;

    @GetMapping("/today")
    public ResponseEntity<DailyPollResponse> getTodayPoll(@AuthenticationPrincipal User user) {
        DailyPollResponse response = pollService.getTodayPoll(user);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/today/vote")
    public ResponseEntity<DailyPollResponse> vote(
            @Valid @RequestBody VoteRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(pollService.vote(request.getRestaurantId(), user));
    }

    @PostMapping("/today/start")
    public ResponseEntity<DailyPollResponse> startTodayPoll(@AuthenticationPrincipal User user) {
        pollService.startTodayPoll();
        return ResponseEntity.ok(pollService.getTodayPoll(user));
    }
}

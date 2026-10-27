package ru.prokin.kitchen.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.prokin.kitchen.dto.DailyPollResponse;
import ru.prokin.kitchen.dto.PollOptionDto;
import ru.prokin.kitchen.dto.UserDto;
import ru.prokin.kitchen.entity.PollVote;
import ru.prokin.kitchen.entity.Restaurant;
import ru.prokin.kitchen.entity.RestaurantPoll;
import ru.prokin.kitchen.entity.User;
import ru.prokin.kitchen.exception.ResourceNotFoundException;
import ru.prokin.kitchen.repository.PollVoteRepository;
import ru.prokin.kitchen.repository.RestaurantPollRepository;
import ru.prokin.kitchen.repository.RestaurantRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PollService {

    private static final Logger logger = LoggerFactory.getLogger(PollService.class);

    private final RestaurantPollRepository pollRepo;
    private final PollVoteRepository voteRepo;
    private final RestaurantRepository restaurantRepo;
    private final TelegramService telegramService;

    @Scheduled(cron = "${poll.cron:0 0 9 * * MON-FRI}")
    public void scheduledMorningPoll() {
        logger.info("Triggering scheduled morning poll for restaurant selection");
        try {
            startTodayPoll();
        } catch (Exception e) {
            logger.error("Failed to run scheduled morning poll", e);
        }
    }

    @Transactional
    public RestaurantPoll startTodayPoll() {
        LocalDate today = LocalDate.now();
        Optional<RestaurantPoll> existing = pollRepo.findByPollDate(today);
        if (existing.isPresent()) {
            return existing.get();
        }

        RestaurantPoll poll = RestaurantPoll.builder()
                .pollDate(today)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
        RestaurantPoll saved = pollRepo.save(poll);

        List<Restaurant> restaurants = restaurantRepo.findAll();
        telegramService.sendPollStartedNotification(restaurants);

        return saved;
    }

    @Transactional(readOnly = true)
    public DailyPollResponse getTodayPoll(User currentUser) {
        LocalDate today = LocalDate.now();
        Optional<RestaurantPoll> pollOpt = pollRepo.findByPollDate(today);
        if (pollOpt.isEmpty()) {
            return null;
        }

        RestaurantPoll poll = pollOpt.get();
        List<Restaurant> allRestaurants = restaurantRepo.findAll();
        List<PollVote> allVotes = voteRepo.findAllWithUserAndRestaurantByPoll(poll);

        int totalVotes = allVotes.size();

        Map<Long, List<PollVote>> votesByRestaurant = allVotes.stream()
                .collect(Collectors.groupingBy(v -> v.getRestaurant().getId()));

        Set<Long> myVotedRestaurantIds = new HashSet<>();
        if (currentUser != null) {
            for (PollVote v : allVotes) {
                if (v.getUser().getId().equals(currentUser.getId())) {
                    myVotedRestaurantIds.add(v.getRestaurant().getId());
                }
            }
        }

        final Set<Long> currentMyVotes = myVotedRestaurantIds;

        List<PollOptionDto> options = allRestaurants.stream().map(r -> {
            List<PollVote> rVotes = votesByRestaurant.getOrDefault(r.getId(), Collections.emptyList());
            int count = rVotes.size();
            double pct = totalVotes > 0 ? Math.round(((double) count / totalVotes) * 1000.0) / 10.0 : 0.0;

            List<UserDto> voters = rVotes.stream()
                    .map(v -> UserDto.fromEntity(v.getUser()))
                    .collect(Collectors.toList());

            boolean isMyVote = currentMyVotes.contains(r.getId());

            return PollOptionDto.builder()
                    .restaurantId(r.getId())
                    .restaurantName(r.getName())
                    .restaurantUrl(r.getWebsiteUrl())
                    .votesCount(count)
                    .percentage(pct)
                    .voters(voters)
                    .isMyVote(isMyVote)
                    .build();
        }).sorted((a, b) -> {
            if (b.getVotesCount() != a.getVotesCount()) {
                return Integer.compare(b.getVotesCount(), a.getVotesCount());
            }
            return a.getRestaurantName().compareToIgnoreCase(b.getRestaurantName());
        }).collect(Collectors.toList());

        Long firstMyVotedId = myVotedRestaurantIds.isEmpty() ? null : myVotedRestaurantIds.iterator().next();

        return DailyPollResponse.builder()
                .id(poll.getId())
                .pollDate(poll.getPollDate())
                .active(poll.isActive())
                .totalVotes(totalVotes)
                .myVotedRestaurantId(firstMyVotedId)
                .myVotedRestaurantIds(new ArrayList<>(myVotedRestaurantIds))
                .options(options)
                .build();
    }

    @Transactional
    public DailyPollResponse vote(Long restaurantId, User user) {
        RestaurantPoll poll = pollRepo.findByPollDate(LocalDate.now())
                .orElseGet(() -> {
                    RestaurantPoll p = RestaurantPoll.builder()
                            .pollDate(LocalDate.now())
                            .active(true)
                            .createdAt(LocalDateTime.now())
                            .build();
                    return pollRepo.save(p);
                });

        Restaurant restaurant = restaurantRepo.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Ресторан с ID " + restaurantId + " не найден"));

        Optional<PollVote> existing = voteRepo.findByPollAndUserAndRestaurant(poll, user, restaurant);
        if (existing.isPresent()) {
            // Повторный клик снимает голос с этого конкретного ресторана
            voteRepo.delete(existing.get());
        } else {
            // Клик добавляет данный ресторан к выбору пользователя (множественный выбор)
            PollVote newVote = PollVote.builder()
                    .poll(poll)
                    .user(user)
                    .restaurant(restaurant)
                    .votedAt(LocalDateTime.now())
                    .build();
            voteRepo.save(newVote);
        }

        return getTodayPoll(user);
    }
}

package ru.prokin.kitchen.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.prokin.kitchen.dto.VoteRequest;
import ru.prokin.kitchen.entity.Restaurant;
import ru.prokin.kitchen.entity.User;
import ru.prokin.kitchen.repository.PollVoteRepository;
import ru.prokin.kitchen.repository.RestaurantPollRepository;
import ru.prokin.kitchen.repository.RestaurantRepository;
import ru.prokin.kitchen.repository.UserRepository;
import ru.prokin.kitchen.security.JwtTokenProvider;
import ru.prokin.kitchen.service.TelegramService;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PollControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private RestaurantPollRepository pollRepository;

    @Autowired
    private PollVoteRepository voteRepository;

    @MockitoBean
    private TelegramService telegramService;

    private User user1;
    private User user2;
    private String token1;
    private String token2;
    private Restaurant r1;
    private Restaurant r2;

    @BeforeEach
    void setUp() {
        user1 = new User();
        user1.setUsername("poll_user1");
        user1.setFullName("Poll User 1");
        user1.setPassword("pwd1");
        user1 = userRepository.save(user1);
        token1 = jwtTokenProvider.generateToken(user1);

        user2 = new User();
        user2.setUsername("poll_user2");
        user2.setFullName("Poll User 2");
        user2.setPassword("pwd2");
        user2 = userRepository.save(user2);
        token2 = jwtTokenProvider.generateToken(user2);

        r1 = new Restaurant();
        r1.setName("Pizza Place");
        r1.setWebsiteUrl("https://pizza.test");
        r1 = restaurantRepository.save(r1);

        r2 = new Restaurant();
        r2.setName("Burger Hub");
        r2.setWebsiteUrl("https://burger.test");
        r2 = restaurantRepository.save(r2);
    }

    @Test
    void testGetTodayPoll_NoContentWhenNoPoll() throws Exception {
        mockMvc.perform(get("/api/v1/polls/today")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isNoContent());
    }

    @Test
    void testStartTodayPoll_Success() throws Exception {
        mockMvc.perform(post("/api/v1/polls/today/start")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive", equalTo(true)))
                .andExpect(jsonPath("$.options.length()", equalTo(2)))
                .andExpect(jsonPath("$.totalVotes", equalTo(0)));
    }

    @Test
    void testVote_Success_And_Toggle() throws Exception {
        // 1. User1 votes for Pizza Place
        VoteRequest req1 = new VoteRequest(r1.getId());
        mockMvc.perform(post("/api/v1/polls/today/vote")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVotes", equalTo(1)))
                .andExpect(jsonPath("$.myVotedRestaurantIds", hasItem(r1.getId().intValue())))
                .andExpect(jsonPath("$.options[0].restaurantId", equalTo(r1.getId().intValue())))
                .andExpect(jsonPath("$.options[0].votesCount", equalTo(1)))
                .andExpect(jsonPath("$.options[0].isMyVote", equalTo(true)));

        // 2. User1 also votes for Burger Hub (multi-choice: both are voted)
        VoteRequest reqBurger = new VoteRequest(r2.getId());
        mockMvc.perform(post("/api/v1/polls/today/vote")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBurger)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVotes", equalTo(2)))
                .andExpect(jsonPath("$.myVotedRestaurantIds", hasItem(r1.getId().intValue())))
                .andExpect(jsonPath("$.myVotedRestaurantIds", hasItem(r2.getId().intValue())));

        // 3. User2 votes for Pizza Place
        VoteRequest req2 = new VoteRequest(r1.getId());
        mockMvc.perform(post("/api/v1/polls/today/vote")
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVotes", equalTo(3)));

        // 4. User1 clicks Pizza Place again -> toggles off r1, keeps r2
        mockMvc.perform(post("/api/v1/polls/today/vote")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVotes", equalTo(2)))
                .andExpect(jsonPath("$.myVotedRestaurantIds", not(hasItem(r1.getId().intValue()))))
                .andExpect(jsonPath("$.myVotedRestaurantIds", hasItem(r2.getId().intValue())));
    }
}

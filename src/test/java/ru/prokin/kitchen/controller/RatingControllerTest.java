package ru.prokin.kitchen.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.prokin.kitchen.entity.User;
import ru.prokin.kitchen.repository.UserRepository;
import ru.prokin.kitchen.security.JwtTokenProvider;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RatingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    private User user;
    private String token;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername("rating_user");
        user.setFullName("Rating User");
        user.setPassword("secret");
        user = userRepository.save(user);

        token = jwtTokenProvider.generateToken(user);
    }

    @Test
    void testGetLeaderboards() throws Exception {
        mockMvc.perform(get("/api/v1/ratings/leaderboards")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topSpentThisMonth").isArray())
                .andExpect(jsonPath("$.topOrdersThisMonth").isArray())
                .andExpect(jsonPath("$.topSpentAllTime").isArray())
                .andExpect(jsonPath("$.topOrdersAllTime").isArray());
    }

    @Test
    void testGetMyStats() throws Exception {
        mockMvc.perform(get("/api/v1/ratings/my-stats")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSpentThisMonth").exists())
                .andExpect(jsonPath("$.totalOrdersThisMonth").exists())
                .andExpect(jsonPath("$.totalSpentAllTime").exists())
                .andExpect(jsonPath("$.totalOrdersAllTime").exists());
    }
}

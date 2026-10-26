package ru.prokin.kitchen.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import ru.prokin.kitchen.dto.AuthResponse;
import ru.prokin.kitchen.dto.LoginRequest;
import ru.prokin.kitchen.dto.RefreshTokenRequest;
import ru.prokin.kitchen.entity.RefreshToken;
import ru.prokin.kitchen.entity.User;
import ru.prokin.kitchen.repository.RefreshTokenRepository;
import ru.prokin.kitchen.repository.UserRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RefreshTokenIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.findByUsername("refresh_test_user")
                .orElseGet(() -> {
                    User user = new User("refresh_test_user", "password123", "Refresh Test User");
                    return userRepository.save(user);
                });
    }

    @Test
    void testLogin_ReturnsAccessTokenAndRefreshToken() throws Exception {
        LoginRequest loginRequest = new LoginRequest("refresh_test_user", "password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andExpect(jsonPath("$.tokenType", equalTo("Bearer")))
                .andExpect(jsonPath("$.user.username", equalTo("refresh_test_user")));
    }

    @Test
    void testRefresh_Success_RotatesRefreshToken() throws Exception {
        // 1. Login to get initial tokens
        LoginRequest loginRequest = new LoginRequest("refresh_test_user", "password123");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(loginResult.getResponse().getContentAsString(), AuthResponse.class);
        String initialRefreshToken = authResponse.getRefreshToken();
        assertNotNull(initialRefreshToken);

        // 2. Refresh tokens
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(initialRefreshToken);
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andExpect(jsonPath("$.user.username", equalTo("refresh_test_user")))
                .andReturn();

        AuthResponse refreshedResponse = objectMapper.readValue(refreshResult.getResponse().getContentAsString(), AuthResponse.class);
        String newRefreshToken = refreshedResponse.getRefreshToken();
        assertNotNull(newRefreshToken);
        assertNotEquals(initialRefreshToken, newRefreshToken, "Refresh token should rotate upon use");

        // 3. Old refresh token should no longer be valid
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(initialRefreshToken))))
                .andExpect(status().isUnauthorized());

        // 4. New refresh token works
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(newRefreshToken))))
                .andExpect(status().isOk());
    }

    @Test
    void testRefresh_ExpiredToken_ReturnsUnauthorized() throws Exception {
        RefreshToken expiredToken = RefreshToken.builder()
                .user(testUser)
                .token("expired-token-12345")
                .createdAt(Instant.now().minus(35, ChronoUnit.DAYS))
                .expiryDate(Instant.now().minus(5, ChronoUnit.DAYS))
                .build();
        refreshTokenRepository.save(expiredToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest("expired-token-12345"))))
                .andExpect(status().isUnauthorized());

        // Ensure expired token is cleaned up
        assertTrue(refreshTokenRepository.findByToken("expired-token-12345").isEmpty());
    }

    @Test
    void testRefresh_InvalidToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest("non-existent-token"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testLogout_RevokesRefreshToken() throws Exception {
        // 1. Login
        LoginRequest loginRequest = new LoginRequest("refresh_test_user", "password123");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(loginResult.getResponse().getContentAsString(), AuthResponse.class);
        String tokenToRevoke = authResponse.getRefreshToken();

        // 2. Logout
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(tokenToRevoke))))
                .andExpect(status().isOk());

        // 3. Trying to refresh with revoked token should fail
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(tokenToRevoke))))
                .andExpect(status().isUnauthorized());
    }
}

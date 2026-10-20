package ru.prokin.kitchen.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testLogin_Success_AutoCreatesFallbackUserInDevMode() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("auto_dev_user");
        request.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tokenType", equalTo("Bearer")))
                .andExpect(jsonPath("$.user.username", equalTo("auto_dev_user")))
                .andExpect(jsonPath("$.user.fullName", equalTo("auto_dev_user")))
                .andExpect(jsonPath("$.user.id").isNumber());
    }

    @Test
    void testLogin_Success_ExistingUserCanLoginAgain() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("repeated_dev_user");
        request.setPassword("securePassword!");

        // First login auto-creates user
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Second login succeeds with same password
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.user.username", equalTo("repeated_dev_user")));
    }

    @Test
    void testLogin_Failure_WrongPasswordReturns401() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("valid_dev_user");
        request.setPassword("correct_pwd");

        // First login creates user
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Login with wrong password
        LoginRequest wrongRequest = new LoginRequest();
        wrongRequest.setUsername("valid_dev_user");
        wrongRequest.setPassword("wrong_pwd");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", equalTo("Unauthorized")));
    }

    @Test
    void testLogin_Failure_ValidationFailsOnBlankInput() throws Exception {
        LoginRequest blankRequest = new LoginRequest();
        blankRequest.setUsername("");
        blankRequest.setPassword("");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", equalTo("Validation Failed")));
    }

    @Test
    void testGetCurrentUser_Success_WithJwt() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("me_user");
        loginRequest.setPassword("mepassword");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                AuthResponse.class
        );
        String token = authResponse.getToken();
        assertNotNull(token);

        // Fetch /api/v1/auth/me with Bearer token
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", equalTo("me_user")))
                .andExpect(jsonPath("$.fullName", equalTo("me_user")));
    }

    @Test
    void testGetCurrentUser_Unauthorized_WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testLinkAd_Success() throws Exception {
        // 1. Initial login
        LoginRequest loginRequest = new LoginRequest("legacy_alice", "alicepass");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(loginResult.getResponse().getContentAsString(), AuthResponse.class);
        String token = authResponse.getToken();

        // 2. Link AD account
        ru.prokin.kitchen.dto.LinkAdRequest linkRequest = new ru.prokin.kitchen.dto.LinkAdRequest("alice.domain", "domainPass");
        mockMvc.perform(post("/api/v1/auth/link-ad")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(linkRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiresAdLink", equalTo(false)))
                .andExpect(jsonPath("$.user.username", equalTo("alice.domain")))
                .andExpect(jsonPath("$.user.adUsername", equalTo("alice.domain")))
                .andExpect(jsonPath("$.user.isAdLinked", equalTo(true)));
    }

    @Test
    void testLinkAd_Conflict_WhenAlreadyLinkedToOtherUser() throws Exception {
        // User 1 links
        LoginRequest login1 = new LoginRequest("legacy_bob", "bobpass");
        MvcResult res1 = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login1)))
                .andExpect(status().isOk())
                .andReturn();
        String token1 = objectMapper.readValue(res1.getResponse().getContentAsString(), AuthResponse.class).getToken();

        ru.prokin.kitchen.dto.LinkAdRequest linkRequest = new ru.prokin.kitchen.dto.LinkAdRequest("shared.ad.user", "domainPass");
        mockMvc.perform(post("/api/v1/auth/link-ad")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(linkRequest)))
                .andExpect(status().isOk());

        // User 2 tries to link the same AD account
        LoginRequest login2 = new LoginRequest("legacy_charlie", "charliepass");
        MvcResult res2 = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login2)))
                .andExpect(status().isOk())
                .andReturn();
        String token2 = objectMapper.readValue(res2.getResponse().getContentAsString(), AuthResponse.class).getToken();

        mockMvc.perform(post("/api/v1/auth/link-ad")
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(linkRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    void testUpdateProfile_Success_UpdatesEmailAndAvatarUrl() throws Exception {
        LoginRequest login = new LoginRequest("profile_user", "password123");
        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.avatarUrl", containsString("gravatar.com/avatar/")))
                .andReturn();
        String token = objectMapper.readValue(res.getResponse().getContentAsString(), AuthResponse.class).getToken();

        ru.prokin.kitchen.dto.UpdateProfileRequest updateRequest = new ru.prokin.kitchen.dto.UpdateProfileRequest("new_custom@gmail.com", "Новое Имя");
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/auth/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", equalTo("new_custom@gmail.com")))
                .andExpect(jsonPath("$.fullName", equalTo("Новое Имя")))
                .andExpect(jsonPath("$.avatarUrl", containsString("gravatar.com/avatar/")));
    }
}

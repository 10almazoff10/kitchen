package ru.prokin.kitchen.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.prokin.kitchen.dto.RestaurantRequest;
import ru.prokin.kitchen.entity.Restaurant;
import ru.prokin.kitchen.entity.User;
import ru.prokin.kitchen.repository.RestaurantRepository;
import ru.prokin.kitchen.repository.UserRepository;
import ru.prokin.kitchen.security.JwtTokenProvider;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RestaurantControllerTest {

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

    private String token;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setUsername("restaurant_admin");
        user.setFullName("Restaurant Admin");
        user.setPassword("pass");
        user = userRepository.save(user);

        token = jwtTokenProvider.generateToken(user);
    }

    @Test
    void testGetAllRestaurants() throws Exception {
        Restaurant r1 = new Restaurant();
        r1.setName("Burger King");
        r1.setWebsiteUrl("https://bk.example");
        restaurantRepository.save(r1);

        mockMvc.perform(get("/api/v1/restaurants")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void testCreateRestaurant() throws Exception {
        RestaurantRequest req = new RestaurantRequest();
        req.setName("Subway");
        req.setWebsiteUrl("https://subway.example");

        mockMvc.perform(post("/api/v1/restaurants")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name", equalTo("Subway")))
                .andExpect(jsonPath("$.websiteUrl", equalTo("https://subway.example")));
    }

    @Test
    void testUpdateRestaurant() throws Exception {
        Restaurant r = new Restaurant();
        r.setName("Old Name");
        r.setWebsiteUrl("https://old.example");
        Restaurant saved = restaurantRepository.save(r);

        RestaurantRequest req = new RestaurantRequest();
        req.setName("New Name");
        req.setWebsiteUrl("https://new.example");

        mockMvc.perform(put("/api/v1/restaurants/" + saved.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", equalTo("New Name")))
                .andExpect(jsonPath("$.websiteUrl", equalTo("https://new.example")));
    }

    @Test
    void testDeleteRestaurant() throws Exception {
        Restaurant r = new Restaurant();
        r.setName("To Delete");
        r.setWebsiteUrl("https://delete.example");
        Restaurant saved = restaurantRepository.save(r);

        mockMvc.perform(delete("/api/v1/restaurants/" + saved.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }
}

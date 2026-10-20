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
import ru.prokin.kitchen.dto.CloseOrderRequest;
import ru.prokin.kitchen.dto.CreateOrderRequest;
import ru.prokin.kitchen.entity.Order;
import ru.prokin.kitchen.entity.Restaurant;
import ru.prokin.kitchen.entity.User;
import ru.prokin.kitchen.repository.OrderRepository;
import ru.prokin.kitchen.repository.RestaurantRepository;
import ru.prokin.kitchen.repository.UserRepository;
import ru.prokin.kitchen.security.JwtTokenProvider;
import ru.prokin.kitchen.service.TelegramService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrderControllerTest {

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
    private OrderRepository orderRepository;

    @MockitoBean
    private TelegramService telegramService;

    private User testUser;
    private String token;
    private Restaurant testRestaurant;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("order_test_user");
        testUser.setFullName("Order Test User");
        testUser.setPassword("password123");
        testUser = userRepository.save(testUser);

        token = jwtTokenProvider.generateToken(testUser);

        testRestaurant = new Restaurant();
        testRestaurant.setName("Sushi Master");
        testRestaurant.setWebsiteUrl("https://sushi.example");
        testRestaurant = restaurantRepository.save(testRestaurant);
    }

    @Test
    void testCreateOrder_Success() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setRestaurantId(testRestaurant.getId());
        request.setDeadlineTime(LocalDateTime.now().plusHours(2));
        request.setPaymentData("Card 1234");
        request.setComment("Need chopsticks");

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.restaurant.name", equalTo("Sushi Master")))
                .andExpect(jsonPath("$.createdBy.username", equalTo("order_test_user")))
                .andExpect(jsonPath("$.acceptingOrders", equalTo(true)))
                .andExpect(jsonPath("$.isAcceptingOrders", equalTo(true)))
                .andExpect(jsonPath("$.closed", equalTo(false)))
                .andExpect(jsonPath("$.isClosed", equalTo(false)));
    }

    @Test
    void testGetActiveOrders() throws Exception {
        Order activeOrder = new Order();
        activeOrder.setRestaurant(testRestaurant);
        activeOrder.setCreatedBy(testUser);
        activeOrder.setDeadlineTime(LocalDateTime.now().plusHours(1));
        activeOrder.setAcceptingOrders(true);
        activeOrder.setClosed(false);
        orderRepository.save(activeOrder);

        mockMvc.perform(get("/api/v1/orders/active")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].restaurant.name", equalTo("Sushi Master")));
    }

    @Test
    void testGetOrderDetail() throws Exception {
        Order order = new Order();
        order.setRestaurant(testRestaurant);
        order.setCreatedBy(testUser);
        order.setDeadlineTime(LocalDateTime.now().plusHours(1));
        order.setAcceptingOrders(true);
        order.setClosed(false);
        Order saved = orderRepository.save(order);

        mockMvc.perform(get("/api/v1/orders/" + saved.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.id", equalTo(saved.getId().intValue())))
                .andExpect(jsonPath("$.order.restaurant.name", equalTo("Sushi Master")))
                .andExpect(jsonPath("$.userSummaries").isArray())
                .andExpect(jsonPath("$.totalAmount").isNumber());
    }

    @Test
    void testToggleAcceptingOrders() throws Exception {
        Order order = new Order();
        order.setRestaurant(testRestaurant);
        order.setCreatedBy(testUser);
        order.setDeadlineTime(LocalDateTime.now().plusHours(1));
        order.setAcceptingOrders(true);
        order.setClosed(false);
        Order saved = orderRepository.save(order);

        mockMvc.perform(post("/api/v1/orders/" + saved.getId() + "/toggle-accepting")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void testCloseOrder_Success() throws Exception {
        Order order = new Order();
        order.setRestaurant(testRestaurant);
        order.setCreatedBy(testUser);
        order.setDeadlineTime(LocalDateTime.now().plusHours(1));
        order.setAcceptingOrders(true);
        order.setClosed(false);
        Order saved = orderRepository.save(order);

        CloseOrderRequest request = new CloseOrderRequest();
        request.setDeliveryCost(new BigDecimal("250.00"));

        mockMvc.perform(post("/api/v1/orders/" + saved.getId() + "/close")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testCreateOrder_UnauthorizedWithoutToken() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setRestaurantId(testRestaurant.getId());
        request.setDeadlineTime(LocalDateTime.now().plusHours(2));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}

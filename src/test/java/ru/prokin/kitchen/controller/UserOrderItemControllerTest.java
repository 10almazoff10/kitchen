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
import ru.prokin.kitchen.dto.AddOrderItemRequest;
import ru.prokin.kitchen.dto.RateItemRequest;
import ru.prokin.kitchen.dto.UpdateOrderItemRequest;
import ru.prokin.kitchen.entity.Order;
import ru.prokin.kitchen.entity.Restaurant;
import ru.prokin.kitchen.entity.User;
import ru.prokin.kitchen.entity.UserOrder;
import ru.prokin.kitchen.repository.OrderRepository;
import ru.prokin.kitchen.repository.RestaurantRepository;
import ru.prokin.kitchen.repository.UserOrderRepository;
import ru.prokin.kitchen.repository.UserRepository;
import ru.prokin.kitchen.security.JwtTokenProvider;
import ru.prokin.kitchen.service.TelegramService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserOrderItemControllerTest {

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

    @Autowired
    private UserOrderRepository userOrderRepository;

    @MockitoBean
    private TelegramService telegramService;

    private User user;
    private String token;
    private Order order;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername("item_user");
        user.setFullName("Item User");
        user.setPassword("pwd123");
        user = userRepository.save(user);

        token = jwtTokenProvider.generateToken(user);

        Restaurant restaurant = new Restaurant();
        restaurant.setName("Wok Bar");
        restaurant.setWebsiteUrl("https://wok.test");
        restaurant = restaurantRepository.save(restaurant);

        order = new Order();
        order.setRestaurant(restaurant);
        order.setCreatedBy(user);
        order.setDeadlineTime(LocalDateTime.now().plusHours(2));
        order.setAcceptingOrders(true);
        order.setClosed(false);
        order = orderRepository.save(order);
    }

    @Test
    void testAddItem_Success() throws Exception {
        AddOrderItemRequest request = new AddOrderItemRequest();
        request.setItemDescription("Pad Thai");
        request.setPrice(new BigDecimal("450.00"));

        mockMvc.perform(post("/api/v1/orders/" + order.getId() + "/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemDescription", equalTo("Pad Thai")))
                .andExpect(jsonPath("$.price", equalTo(450.00)))
                .andExpect(jsonPath("$.paid", equalTo(false)))
                .andExpect(jsonPath("$.addedToRestaurantOrder", equalTo(false)));
    }

    @Test
    void testUpdateItem_Success() throws Exception {
        UserOrder item = new UserOrder();
        item.setOrder(order);
        item.setUser(user);
        item.setItemDescription("Tom Yum");
        item.setPrice(new BigDecimal("350.00"));
        item = userOrderRepository.save(item);

        UpdateOrderItemRequest request = new UpdateOrderItemRequest();
        request.setItemDescription("Tom Yum Kung Extra Spicy");
        request.setPrice(new BigDecimal("400.00"));

        mockMvc.perform(put("/api/v1/items/" + item.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemDescription", equalTo("Tom Yum Kung Extra Spicy")))
                .andExpect(jsonPath("$.price", equalTo(400.00)));
    }

    @Test
    void testTogglePaid_Success() throws Exception {
        UserOrder item = new UserOrder();
        item.setOrder(order);
        item.setUser(user);
        item.setItemDescription("Spring Rolls");
        item.setPrice(new BigDecimal("200.00"));
        item.setPaid(false);
        item = userOrderRepository.save(item);

        mockMvc.perform(patch("/api/v1/items/" + item.getId() + "/toggle-paid")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paid", equalTo(true)));
    }

    @Test
    void testToggleAdded_Success() throws Exception {
        UserOrder item = new UserOrder();
        item.setOrder(order);
        item.setUser(user);
        item.setItemDescription("Fried Rice");
        item.setPrice(new BigDecimal("300.00"));
        item.setAddedToRestaurantOrder(false);
        item = userOrderRepository.save(item);

        mockMvc.perform(patch("/api/v1/items/" + item.getId() + "/toggle-added")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addedToRestaurantOrder", equalTo(true)));
    }

    @Test
    void testRateItem_Success() throws Exception {
        UserOrder item = new UserOrder();
        item.setOrder(order);
        item.setUser(user);
        item.setItemDescription("Miso Soup");
        item.setPrice(new BigDecimal("150.00"));
        item = userOrderRepository.save(item);

        RateItemRequest request = new RateItemRequest();
        request.setRating(5);

        mockMvc.perform(post("/api/v1/items/" + item.getId() + "/rate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating", equalTo(5)));
    }

    @Test
    void testGetUnpaidOrders_Success() throws Exception {
        UserOrder item = new UserOrder();
        item.setOrder(order);
        item.setUser(user);
        item.setItemDescription("Unpaid item");
        item.setPrice(new BigDecimal("250.00"));
        item.setPaid(false);
        userOrderRepository.save(item);

        mockMvc.perform(get("/api/v1/orders/unpaid")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount", equalTo(1)))
                .andExpect(jsonPath("$.totalDebt", equalTo(250.00)));
    }

    @Test
    void testDeleteItem_Success() throws Exception {
        UserOrder item = new UserOrder();
        item.setOrder(order);
        item.setUser(user);
        item.setItemDescription("To Delete");
        item.setPrice(new BigDecimal("100.00"));
        item = userOrderRepository.save(item);

        mockMvc.perform(delete("/api/v1/items/" + item.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void testGetPreviousItems_Success() throws Exception {
        // First order of Pad Thai with price 400
        UserOrder item1 = new UserOrder();
        item1.setOrder(order);
        item1.setUser(user);
        item1.setItemDescription("Pad Thai");
        item1.setPrice(new BigDecimal("400.00"));
        userOrderRepository.save(item1);

        // Order of Tom Yum
        UserOrder item2 = new UserOrder();
        item2.setOrder(order);
        item2.setUser(user);
        item2.setItemDescription("Tom Yum");
        item2.setPrice(new BigDecimal("500.00"));
        userOrderRepository.save(item2);

        // Later order of Pad Thai with new price 450
        UserOrder item3 = new UserOrder();
        item3.setOrder(order);
        item3.setUser(user);
        item3.setItemDescription("Pad Thai");
        item3.setPrice(new BigDecimal("450.00"));
        userOrderRepository.save(item3);

        // Other user's item (should not be in results)
        User otherUser = new User();
        otherUser.setUsername("other_user");
        otherUser.setFullName("Other User");
        otherUser.setPassword("pwd");
        otherUser = userRepository.save(otherUser);

        UserOrder otherUserItem = new UserOrder();
        otherUserItem.setOrder(order);
        otherUserItem.setUser(otherUser);
        otherUserItem.setItemDescription("Fried Rice");
        otherUserItem.setPrice(new BigDecimal("300.00"));
        userOrderRepository.save(otherUserItem);

        // Other restaurant's item for current user (should not be in results)
        Restaurant otherRestaurant = new Restaurant();
        otherRestaurant.setName("Burger Place");
        otherRestaurant.setWebsiteUrl("https://burger.test");
        otherRestaurant = restaurantRepository.save(otherRestaurant);

        Order otherOrder = new Order();
        otherOrder.setRestaurant(otherRestaurant);
        otherOrder.setCreatedBy(user);
        otherOrder.setDeadlineTime(LocalDateTime.now().plusHours(1));
        otherOrder = orderRepository.save(otherOrder);

        UserOrder otherRestItem = new UserOrder();
        otherRestItem.setOrder(otherOrder);
        otherRestItem.setUser(user);
        otherRestItem.setItemDescription("Cheeseburger");
        otherRestItem.setPrice(new BigDecimal("350.00"));
        userOrderRepository.save(otherRestItem);

        // Test GET /api/v1/restaurants/{id}/previous-items
        mockMvc.perform(get("/api/v1/restaurants/" + order.getRestaurant().getId() + "/previous-items")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", equalTo(2)))
                .andExpect(jsonPath("$[0].itemDescription", equalTo("Pad Thai")))
                .andExpect(jsonPath("$[0].price", equalTo(450.00)))
                .andExpect(jsonPath("$[1].itemDescription", equalTo("Tom Yum")))
                .andExpect(jsonPath("$[1].price", equalTo(500.00)));

        // Test GET /api/v1/orders/{orderId}/previous-items
        mockMvc.perform(get("/api/v1/orders/" + order.getId() + "/previous-items")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", equalTo(2)))
                .andExpect(jsonPath("$[0].itemDescription", equalTo("Pad Thai")))
                .andExpect(jsonPath("$[0].price", equalTo(450.00)));
    }

    @Test
    void testGetPreviousItems_EmptyWhenNoOrders() throws Exception {
        Restaurant emptyRestaurant = new Restaurant();
        emptyRestaurant.setName("Empty Cafe");
        emptyRestaurant.setWebsiteUrl("https://empty.test");
        emptyRestaurant = restaurantRepository.save(emptyRestaurant);

        mockMvc.perform(get("/api/v1/restaurants/" + emptyRestaurant.getId() + "/previous-items")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", equalTo(0)));
    }
}

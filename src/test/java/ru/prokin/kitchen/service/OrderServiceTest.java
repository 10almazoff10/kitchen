package ru.prokin.kitchen.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import ru.prokin.kitchen.dto.CreateOrderRequest;
import ru.prokin.kitchen.dto.OrderDetailResponse;
import ru.prokin.kitchen.dto.OrderResponse;
import ru.prokin.kitchen.dto.UserOrderSummaryDto;
import ru.prokin.kitchen.entity.Order;
import ru.prokin.kitchen.entity.Restaurant;
import ru.prokin.kitchen.entity.User;
import ru.prokin.kitchen.entity.UserOrder;
import ru.prokin.kitchen.exception.BadRequestException;
import ru.prokin.kitchen.repository.OrderRepository;
import ru.prokin.kitchen.repository.RestaurantRepository;
import ru.prokin.kitchen.repository.UserOrderRepository;
import ru.prokin.kitchen.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserOrderRepository userOrderRepository;

    @MockitoBean
    private TelegramService telegramService;

    private User creator;
    private User participant;
    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        creator = new User();
        creator.setUsername("order_creator");
        creator.setFullName("Order Creator");
        creator.setPassword("secret");
        creator = userRepository.save(creator);

        participant = new User();
        participant.setUsername("participant_user");
        participant.setFullName("Participant User");
        participant.setPassword("secret");
        participant = userRepository.save(participant);

        restaurant = new Restaurant();
        restaurant.setName("Test Pizzeria");
        restaurant.setWebsiteUrl("https://pizzeria.test");
        restaurant = restaurantRepository.save(restaurant);
    }

    @Test
    void testCreateOrder() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setRestaurantId(restaurant.getId());
        request.setDeadlineTime(LocalDateTime.now().plusHours(2));
        request.setPaymentData("Sber +79991234567");
        request.setComment("Don't forget napkins");

        OrderResponse response = orderService.createOrder(request, creator);

        assertNotNull(response.getId());
        assertEquals("Test Pizzeria", response.getRestaurant().getName());
        assertTrue(response.isAcceptingOrders());
        assertFalse(response.isClosed());
    }

    @Test
    void testDistributeDeliveryCostOnCloseOrder() {
        // Create an order
        Order order = new Order();
        order.setRestaurant(restaurant);
        order.setCreatedBy(creator);
        order.setDeadlineTime(LocalDateTime.now().plusHours(1));
        order.setPaymentData("+79991112233");
        order.setAcceptingOrders(true);
        order.setClosed(false);
        order = orderRepository.save(order);

        // Creator has 2 items
        UserOrder item1 = new UserOrder();
        item1.setOrder(order);
        item1.setUser(creator);
        item1.setItemDescription("Pizza Margherita");
        item1.setPrice(new BigDecimal("500.00"));
        userOrderRepository.save(item1);

        UserOrder item2 = new UserOrder();
        item2.setOrder(order);
        item2.setUser(creator);
        item2.setItemDescription("Cola 0.5");
        item2.setPrice(new BigDecimal("100.00"));
        userOrderRepository.save(item2);

        // Participant has 1 item
        UserOrder item3 = new UserOrder();
        item3.setOrder(order);
        item3.setUser(participant);
        item3.setItemDescription("Burger Deluxe");
        item3.setPrice(new BigDecimal("350.00"));
        userOrderRepository.save(item3);

        // Total delivery cost is 300 across 2 users -> 150 per user
        BigDecimal totalDeliveryCost = new BigDecimal("300.00");
        orderService.closeOrder(order.getId(), totalDeliveryCost, creator);

        // Verify order is closed
        Order closedOrder = orderService.getOrderEntity(order.getId());
        assertTrue(closedOrder.isClosed());
        assertFalse(closedOrder.isAcceptingOrders());

        // Verify detail response
        OrderDetailResponse detail = orderService.getOrderDetail(order.getId());
        assertEquals(2, detail.getUserSummaries().size());

        // Total items = 500 + 100 + 350 = 950
        // Total delivery = 300
        // Grand total = 1250
        assertEquals(new BigDecimal("300.00"), detail.getTotalDeliveryCost());
        assertEquals(new BigDecimal("1250.00"), detail.getTotalAmount());

        // Verify each user's delivery cost
        UserOrderSummaryDto creatorSummary = detail.getUserSummaries().stream()
                .filter(s -> s.getUserId().equals(creator.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(new BigDecimal("150.00"), creatorSummary.getDeliveryCost());
        assertEquals(new BigDecimal("750.00"), creatorSummary.getTotalAmount()); // 600 items + 150 delivery

        UserOrderSummaryDto participantSummary = detail.getUserSummaries().stream()
                .filter(s -> s.getUserId().equals(participant.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(new BigDecimal("150.00"), participantSummary.getDeliveryCost());
        assertEquals(new BigDecimal("500.00"), participantSummary.getTotalAmount()); // 350 items + 150 delivery
    }

    @Test
    void testCloseOrder_NonCreatorCannotClose() {
        Order order = new Order();
        order.setRestaurant(restaurant);
        order.setCreatedBy(creator);
        order.setDeadlineTime(LocalDateTime.now().plusHours(1));
        order.setAcceptingOrders(true);
        order.setClosed(false);
        Order saved = orderRepository.save(order);
        Long orderId = saved.getId();

        assertThrows(BadRequestException.class, () ->
                orderService.closeOrder(orderId, new BigDecimal("100"), participant)
        );
    }

    @Test
    void testCloseOrder_CannotCloseAlreadyClosedOrder() {
        Order order = new Order();
        order.setRestaurant(restaurant);
        order.setCreatedBy(creator);
        order.setDeadlineTime(LocalDateTime.now().plusHours(1));
        order.setAcceptingOrders(true);
        order.setClosed(true);
        Order saved = orderRepository.save(order);
        Long orderId = saved.getId();

        assertThrows(BadRequestException.class, () ->
                orderService.closeOrder(orderId, new BigDecimal("100"), creator)
        );
    }

    @Test
    void testToggleAcceptingOrders() {
        Order order = new Order();
        order.setRestaurant(restaurant);
        order.setCreatedBy(creator);
        order.setDeadlineTime(LocalDateTime.now().plusHours(1));
        order.setAcceptingOrders(true);
        order.setClosed(false);
        Order saved = orderRepository.save(order);

        orderService.toggleAcceptingOrders(saved.getId(), creator);
        Order updated = orderService.getOrderEntity(saved.getId());
        assertFalse(updated.isAcceptingOrders());

        orderService.toggleAcceptingOrders(saved.getId(), creator);
        updated = orderService.getOrderEntity(saved.getId());
        assertTrue(updated.isAcceptingOrders());
    }

    @Test
    void testToggleAcceptingOrders_NonCreatorThrowsBadRequest() {
        Order order = new Order();
        order.setRestaurant(restaurant);
        order.setCreatedBy(creator);
        order.setDeadlineTime(LocalDateTime.now().plusHours(1));
        order.setAcceptingOrders(true);
        order.setClosed(false);
        Order saved = orderRepository.save(order);
        Long orderId = saved.getId();

        assertThrows(BadRequestException.class, () ->
                orderService.toggleAcceptingOrders(orderId, participant)
        );
    }
}

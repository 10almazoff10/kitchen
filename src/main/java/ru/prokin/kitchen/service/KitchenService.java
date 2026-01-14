package ru.prokin.kitchen.service;

import org.springframework.beans.factory.annotation.Value;
import ru.prokin.kitchen.entity.*;
import ru.prokin.kitchen.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import java.math.BigDecimal;
import java.util.List;

@Service
public class KitchenService {

    @Autowired private UserRepository userRepo;
    @Autowired private OrderRepository orderRepo;
    @Autowired private UserOrderRepository userOrderRepo;
    @Autowired private UserService userService;
    @Autowired private RestaurantRepository restaurantRepo;
    @Autowired private TelegramService telegramService;

    @Value("${kitchen.base-url}")
    private String baseUrl;

    public List<Order> getActiveOrders() {
        return orderRepo.findByIsClosedFalseOrderByIdDesc();
    }

    public Page<Order> getClosedOrders(Pageable pageable) {
        return orderRepo.findByIsClosedTrueOrderByIdDesc(pageable);
    }
    public List<Order> getOrders(){ return orderRepo.findAll();}

    public Order createOrder(Restaurant restaurant, java.time.LocalDateTime deadline, User createdBy, String paymentData) {
        Order order = new Order();
        order.setRestaurant(restaurant);
        order.setDeadlineTime(deadline);
        order.setCreatedBy(createdBy);
        order.setPaymentData(paymentData);
        Order savedOrder = orderRepo.save(order);

        // Отправляем сообщение в Telegram
        String message = String.format(
                "🔔 <b>Новый заказ!</b>\n" +
                        "👤 Создал: %s\n" +
                        "🍽 Ресторан: %s\n" +
                        "⏰ Дедлайн: %s\n" +
                        "💳 Для оплаты: %s\n" +
                        "🔗 <a href=\"%s/order/%d\">Перейти к заказу</a>",
                createdBy.getFullName(),
                restaurant.getName(),
                deadline.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                paymentData,
                baseUrl,
                savedOrder.getId()
        );
        telegramService.sendMessage(message);

        return savedOrder;
    }

    public void addUserOrder(Long orderId, Long userId, String item, java.math.BigDecimal price) {
        Order order = orderRepo.findById(orderId).orElseThrow();
        User user = userRepo.findById(userId).orElseThrow();
        UserOrder userOrder = new UserOrder();
        userOrder.setOrder(order);
        userOrder.setUser(user); // user из app_users
        userOrder.setItemDescription(item);
        userOrder.setPrice(price);
        userOrderRepo.save(userOrder);
    }

    public void deleteUserOrder(Long userOrderId) {
        UserOrder userOrder = userOrderRepo.findById(userOrderId).orElseThrow(() -> new RuntimeException("UserOrder not found"));
        userOrderRepo.delete(userOrder);
    }

    public void closeOrder(Long orderId) {
        Order order = orderRepo.findById(orderId).orElseThrow();
        order.setClosed(true);
        orderRepo.save(order);
        telegramService.sendDeadlineNotification(order);

    }

    public List<UserOrder> getUsersItemsInOrder(Long orderId) {
        return userOrderRepo.findWithUserByOrder_Id(orderId);
    }

    public void markUserOrderAsAdded(Long userOrderId) {
        UserOrder userOrder = userOrderRepo.findById(userOrderId).orElseThrow(() -> new RuntimeException("UserOrder not found"));
        userOrder.setAddedToRestaurantOrder(true);
        userOrderRepo.save(userOrder);
    }

    public UserOrder getUserOrderByUserOrderId(Long userOrderId) {
        return userOrderRepo.findById(userOrderId).orElseThrow(() -> new RuntimeException("UserOrder not found"));
    }

    public void markUserOrderAsPaid(Long userOrderId) {
        UserOrder userOrder = userOrderRepo.findById(userOrderId).orElseThrow(() -> new RuntimeException("UserOrder not found"));
        userOrder.setPaid(true);
        userOrderRepo.save(userOrder);
    }

    public BigDecimal getTotalAmountForOrder(Long orderId) {
        List<UserOrder> userOrders = userOrderRepo.findByOrder_IdOrderByIdAsc(orderId);
        return userOrders.stream()
                .map(UserOrder::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void updateUserOrder(Long userOrderId, String itemDescription, BigDecimal price) {
        UserOrder userOrder = userOrderRepo.findById(userOrderId).orElseThrow(() -> new RuntimeException("UserOrder not found"));
        userOrder.setItemDescription(itemDescription);
        userOrder.setPrice(price);
        userOrderRepo.save(userOrder);
    }

    public void stopAcceptingOrders(Long orderId) {
        Order order = orderRepo.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        order.setAcceptingOrders(false);
        orderRepo.save(order);
    }

    // Обновлённый метод
    public User loadUserByUsername(String username) {
        return (User) userService.loadUserByUsername(username);
    }

    public Order getOrderById(Long id) {
        return orderRepo.findById(id).orElseThrow(() -> new RuntimeException("Order not found"));
    }

    public List<Restaurant> getAllRestaurants() {
        return restaurantRepo.findAll();
    }

    public Restaurant getRestaurantById(Long id) {
        return restaurantRepo.findById(id).orElseThrow(() -> new RuntimeException("Restaurant not found"));
    }

    public void saveRestaurant(Restaurant restaurant) {
        restaurantRepo.save(restaurant);
    }

    @Transactional
    public void updateRating(Long userOrderId, Integer rating, Long userId) {
        UserOrder userOrder = userOrderRepo.findById(userOrderId).orElseThrow(() -> new RuntimeException("UserOrder not found"));

        // Проверяем, что пользователь может оценить только своё блюдо
        if (!userOrder.getUser().getId().equals(userId)) {
            throw new RuntimeException("You can only rate your own items");
        }

        // Проверяем, что оценка от 1 до 5
        if (rating < 1 || rating > 5) {
            throw new RuntimeException("Rating must be between 1 and 5");
        }

        userOrderRepo.updateRating(userOrderId, rating);
    }
}
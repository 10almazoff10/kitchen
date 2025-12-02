package ru.softlogic.paylogic_kitchen.service;

import ru.softlogic.paylogic_kitchen.entity.*;
import ru.softlogic.paylogic_kitchen.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public List<Order> getActiveOrders() {
        return orderRepo.findByIsClosedFalseOrderByIdDesc();
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
                        "🔗 <a href=\"http://your-domain.com/order/%d\">Перейти к заказу</a>",
                createdBy.getFullName(),
                restaurant.getName(),
                deadline.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                paymentData,
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
        List<UserOrder> userOrders = userOrderRepo.findByOrder_Id(orderId);
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

    public List<Order> getClosedOrders() {
        return orderRepo.findByIsClosedTrue();
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
}
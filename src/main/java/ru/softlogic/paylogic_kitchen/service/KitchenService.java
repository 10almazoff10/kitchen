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

    public User findOrCreateUser(String name) {
        return userRepo.findByName(name).orElseGet(() -> {
            User user = new User(name);
            return userRepo.save(user);
        });
    }

    public List<Order> getActiveOrders() {
        return orderRepo.findByIsClosedFalse();
    }

    public List<Order> getOrders(){ return orderRepo.findAll();}


    public Order createOrder(String restaurantUrl, java.time.LocalDateTime deadline, User createdBy, String paymentData) {
        Order order = new Order();
        order.setRestaurantUrl(restaurantUrl);
        order.setDeadlineTime(deadline);
        order.setCreatedBy(createdBy);
        order.setPaymentData(paymentData); // добавили
        return orderRepo.save(order);
    }

    public void closeOrder(Long orderId) {
        Order order = orderRepo.findById(orderId).orElseThrow();
        order.setClosed(true);
        orderRepo.save(order);
    }

    public void addUserOrder(Long orderId, Long userId, String item, java.math.BigDecimal price) {
        Order order = orderRepo.findById(orderId).orElseThrow();
        User user = userRepo.findById(userId).orElseThrow();

        UserOrder userOrder = new UserOrder();
        userOrder.setOrder(order);
        userOrder.setUser(user);
        userOrder.setItemDescription(item);
        userOrder.setPrice(price);

        userOrderRepo.save(userOrder);
    }
    public List<UserOrder> getUsersItemsInOrder(Long orderId) {
        Order order = orderRepo.findById(orderId).orElseThrow();
        return userOrderRepo.getUserOrdersByOrder(order);

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
}
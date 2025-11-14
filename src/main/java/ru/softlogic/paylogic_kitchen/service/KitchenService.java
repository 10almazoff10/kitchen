package ru.softlogic.paylogic_kitchen.service;

import ru.softlogic.paylogic_kitchen.entity.*;
import ru.softlogic.paylogic_kitchen.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public Order createOrder(String restaurantUrl, java.time.LocalDateTime deadline, User createdBy) {
        Order order = new Order();
        order.setRestaurantUrl(restaurantUrl);
        order.setDeadlineTime(deadline);
        order.setCreatedBy(createdBy);
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
}
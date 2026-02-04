package ru.prokin.kitchen.service;

import org.springframework.beans.factory.annotation.Value;
import ru.prokin.kitchen.dto.UserOrderSummary;
import ru.prokin.kitchen.entity.*;
import ru.prokin.kitchen.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public void createOrder(Restaurant restaurant, java.time.LocalDateTime deadline, User createdBy, String paymentData, String comment) {
        Order order = new Order();
        order.setRestaurant(restaurant);
        order.setDeadlineTime(deadline);
        order.setCreatedBy(createdBy);
        order.setPaymentData(paymentData);
        order.setComment(comment);
        Order savedOrder = orderRepo.save(order);

        // Отправляем сообщение в Telegram
        String message = String.format(
                "🔔 <b>Новый заказ!</b>\n" +
                        "🔗 <a href=\"%s/order/%d\">Перейти к заказу</a>\n" +
                        "👤 Создал: %s\n" +
                        "🍽 Ресторан: %s\n" +
                        "⏰ Дедлайн: %s\n" +
                        "💳 Для оплаты: %s\n" +
                        "💬 Комментарий: %s",
                baseUrl,
                savedOrder.getId(),
                createdBy.getFullName(),
                restaurant.getName(),
                deadline.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                paymentData,
                comment
        );
        telegramService.sendMessage(message);
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

    public void closeOrder(Long orderId, BigDecimal deliveryCost) {
        Order order = orderRepo.findById(orderId).orElseThrow();

        // Распределяем стоимость доставки между пользователями
        distributeDeliveryCost(orderId, deliveryCost);

        order.setClosed(true);
        orderRepo.save(order);
        telegramService.sendDeadlineNotification(order);
    }

    private void distributeDeliveryCost(Long orderId, BigDecimal deliveryCost) {
        if (deliveryCost.compareTo(BigDecimal.ZERO) <= 0) {
            // Если стоимость доставки не указана или равна 0, не распределяем
            return;
        }

        // Получаем все заказы пользователей в этом заказе
        List<UserOrder> userOrders = userOrderRepo.findByOrder_IdOrderByIdAsc(orderId);

        if (userOrders.isEmpty()) {
            // Если нет пользовательских заказов, нечего распределять
            return;
        }

        // Находим уникальных пользователей
        Map<Long, List<UserOrder>> ordersByUser = userOrders.stream()
            .collect(Collectors.groupingBy(uo -> uo.getUser().getId()));

        int uniqueUsersCount = ordersByUser.size();

        if (uniqueUsersCount == 0) {
            return;
        }

        // Вычисляем стоимость доставки на каждого пользователя
        BigDecimal deliveryPerUser = deliveryCost.divide(new BigDecimal(uniqueUsersCount), java.math.RoundingMode.HALF_UP);

        // Присваиваем стоимость доставки первому блюду каждого пользователя,
        // остальным блюдам этого пользователя присваиваем 0
        for (List<UserOrder> userOrderList : ordersByUser.values()) {
            boolean isFirstItem = true;
            for (UserOrder userOrder : userOrderList) {
                if (isFirstItem) {
                    userOrder.setDeliveryCost(deliveryPerUser);
                    isFirstItem = false;
                } else {
                    userOrder.setDeliveryCost(BigDecimal.ZERO);
                }
                userOrderRepo.save(userOrder);
            }
        }
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

        // Подсчет общей стоимости блюд
        BigDecimal totalItemsPrice = userOrders.stream()
                .map(UserOrder::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Подсчет общей стоимости доставки (учитываем только по одному разу для каждого пользователя)
        Map<Long, List<UserOrder>> ordersByUser = userOrders.stream()
            .collect(Collectors.groupingBy(uo -> uo.getUser().getId()));

        BigDecimal totalDeliveryCost = ordersByUser.values().stream()
            .map(userOrderList -> userOrderList.get(0).getDeliveryCost()) // Берем доставку только из первого элемента каждого пользователя
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalItemsPrice.add(totalDeliveryCost);
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

    public List<UserOrderSummary> getUserOrderSummariesByOrderId(Long orderId) {
        List<UserOrder> orderItems = getUsersItemsInOrder(orderId);

        Map<Long, List<UserOrder>> groupedByUser = orderItems.stream()
            .collect(Collectors.groupingBy(item -> item.getUser().getId()));

        return groupedByUser.values().stream().map(items -> {
            UserOrder first = items.get(0);

            // Берем стоимость доставки только первого элемента, так как она уже распределена
            // между пользователями, а не между их блюдами
            BigDecimal deliveryCost = items.get(0).getDeliveryCost();

            boolean allPaid = items.stream().allMatch(UserOrder::isPaid);
            boolean allAdded = items.stream().allMatch(UserOrder::isAddedToRestaurantOrder);

            return new UserOrderSummary(
                first.getUser().getId(),
                first.getUser().getFullName(),
                items,
                deliveryCost,
                allPaid,
                allAdded
            );
        }).collect(Collectors.toList());
    }

    @Transactional
    public void markAllItemsPaidByUser(Long orderId, Long userId) {
        List<UserOrder> userOrders = userOrderRepo.findByOrder_IdAndUser_Id(orderId, userId);
        for (UserOrder userOrder : userOrders) {
            userOrder.setPaid(true);
            userOrderRepo.save(userOrder);
        }
    }

    @Transactional
    public void markAllItemsAddedByUser(Long orderId, Long userId) {
        List<UserOrder> userOrders = userOrderRepo.findByOrder_IdAndUser_Id(orderId, userId);
        for (UserOrder userOrder : userOrders) {
            userOrder.setAddedToRestaurantOrder(true);
            userOrderRepo.save(userOrder);
        }
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
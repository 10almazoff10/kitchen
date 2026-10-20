package ru.prokin.kitchen.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;
import ru.prokin.kitchen.dto.*;
import ru.prokin.kitchen.entity.*;
import ru.prokin.kitchen.exception.BadRequestException;
import ru.prokin.kitchen.exception.ResourceNotFoundException;
import ru.prokin.kitchen.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final UserOrderRepository userOrderRepo;
    private final RestaurantRepository restaurantRepo;
    private final TelegramService telegramService;

    @Value("${kitchen.base-url:http://localhost}")
    private String baseUrl;

    public List<OrderResponse> getActiveOrders() {
        return orderRepo.findByIsClosedFalseOrderByIdDesc().stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    public Page<OrderResponse> getClosedOrders(Pageable pageable) {
        return orderRepo.findByIsClosedTrueOrderByIdDesc(pageable)
                .map(this::mapToOrderResponse);
    }

    public Order getOrderEntity(Long id) {
        return orderRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Заказ с ID " + id + " не найден"));
    }

    public OrderDetailResponse getOrderDetail(Long id) {
        Order order = getOrderEntity(id);
        List<UserOrder> userOrders = userOrderRepo.findWithUserByOrder_Id(id);

        Map<Long, List<UserOrder>> byUser = userOrders.stream()
                .collect(Collectors.groupingBy(uo -> uo.getUser().getId(), LinkedHashMap::new, Collectors.toList()));

        List<UserOrderSummaryDto> userSummaries = byUser.entrySet().stream()
                .map(entry -> {
                    List<UserOrder> items = entry.getValue();
                    User user = items.get(0).getUser();
                    BigDecimal itemsTotal = items.stream()
                            .map(UserOrder::getPrice)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal delivery = items.get(0).getDeliveryCost() != null ? items.get(0).getDeliveryCost() : BigDecimal.ZERO;
                    boolean allPaid = items.stream().allMatch(UserOrder::isPaid);
                    boolean allAdded = items.stream().allMatch(UserOrder::isAddedToRestaurantOrder);

                    List<OrderItemResponse> itemDtos = items.stream()
                            .map(OrderItemResponse::fromEntity)
                            .collect(Collectors.toList());

                    return UserOrderSummaryDto.builder()
                            .userId(user.getId())
                            .userFullName(user.getFullName())
                            .avatarUrl(user.getAvatarUrl())
                            .items(itemDtos)
                            .deliveryCost(delivery)
                            .totalAmount(itemsTotal.add(delivery))
                            .allPaid(allPaid)
                            .allAdded(allAdded)
                            .build();
                })
                .collect(Collectors.toList());

        BigDecimal totalItemsPrice = userOrders.stream()
                .map(UserOrder::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDelivery = userSummaries.stream()
                .map(UserOrderSummaryDto::getDeliveryCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return OrderDetailResponse.builder()
                .order(OrderResponse.fromEntity(order, totalItemsPrice.add(totalDelivery), userOrders.size()))
                .userSummaries(userSummaries)
                .totalAmount(totalItemsPrice.add(totalDelivery))
                .totalDeliveryCost(totalDelivery)
                .build();
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, User createdBy) {
        Restaurant restaurant = restaurantRepo.findById(request.getRestaurantId())
                .orElseThrow(() -> new ResourceNotFoundException("Ресторан с ID " + request.getRestaurantId() + " не найден"));

        Order order = new Order();
        order.setRestaurant(restaurant);
        order.setDeadlineTime(request.getDeadlineTime());
        order.setCreatedBy(createdBy);
        order.setPaymentData(request.getPaymentData());
        order.setComment(request.getComment());
        order.setAcceptingOrders(true);
        order.setClosed(false);

        Order saved = orderRepo.save(order);

        try {
            String orderUrl = buildOrderUrl(saved.getId());
            String createdByName = createdBy.getFullName() != null ? HtmlUtils.htmlEscape(createdBy.getFullName()) : "Аноним";
            String restaurantName = restaurant.getName() != null ? HtmlUtils.htmlEscape(restaurant.getName()) : "-";
            String paymentData = order.getPaymentData() != null ? HtmlUtils.htmlEscape(order.getPaymentData()) : "-";
            String comment = order.getComment() != null ? HtmlUtils.htmlEscape(order.getComment()) : "-";

            String message = String.format(
                    "🔔 <b>Новый заказ!</b>\n" +
                            "🔗 <a href=\"%s\">Перейти к заказу</a>\n" +
                            "👤 Создал: %s\n" +
                            "🍽 Ресторан: %s\n" +
                            "⏰ Дедлайн: %s\n" +
                            "💳 Для оплаты: %s\n" +
                            "💬 Комментарий: %s",
                    orderUrl,
                    createdByName,
                    restaurantName,
                    order.getDeadlineTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                    paymentData,
                    comment
            );
            telegramService.sendMessage(message);
        } catch (Exception ignored) {}

        return mapToOrderResponse(saved);
    }

    private String buildOrderUrl(Long orderId) {
        String cleanBaseUrl = baseUrl != null ? baseUrl.trim().replaceAll("/+$", "") : "http://localhost:8080";
        if (!cleanBaseUrl.startsWith("http://") && !cleanBaseUrl.startsWith("https://")) {
            cleanBaseUrl = "http://" + cleanBaseUrl;
        }
        return cleanBaseUrl + "/orders/" + orderId;
    }

    @Transactional
    public void toggleAcceptingOrders(Long orderId, User user) {
        Order order = getOrderEntity(orderId);
        if (!order.getCreatedBy().getId().equals(user.getId())) {
            throw new BadRequestException("Только создатель заказа может менять статус приема");
        }
        order.setAcceptingOrders(!order.isAcceptingOrders());
        orderRepo.save(order);
    }

    @Transactional
    public void updateComment(Long orderId, String comment, User user) {
        Order order = getOrderEntity(orderId);
        if (!order.getCreatedBy().getId().equals(user.getId())) {
            throw new BadRequestException("Только создатель заказа может изменять комментарий");
        }
        order.setComment(comment != null ? comment.trim() : null);
        orderRepo.save(order);
    }

    @Transactional
    public void closeOrder(Long orderId, BigDecimal deliveryCost, User user) {
        Order order = getOrderEntity(orderId);
        if (!order.getCreatedBy().getId().equals(user.getId())) {
            throw new BadRequestException("Только создатель заказа может закрыть заказ");
        }
        if (order.isClosed()) {
            throw new BadRequestException("Заказ уже закрыт");
        }

        distributeDeliveryCost(orderId, deliveryCost != null ? deliveryCost : BigDecimal.ZERO);
        order.setClosed(true);
        order.setAcceptingOrders(false);
        orderRepo.save(order);

        try {
            telegramService.sendDeadlineNotification(order);
        } catch (Exception ignored) {}
    }

    private void distributeDeliveryCost(Long orderId, BigDecimal deliveryCost) {
        if (deliveryCost.compareTo(BigDecimal.ZERO) <= 0) return;

        List<UserOrder> userOrders = userOrderRepo.findByOrder_IdOrderByIdAsc(orderId);
        if (userOrders.isEmpty()) return;

        Map<Long, List<UserOrder>> ordersByUser = userOrders.stream()
                .collect(Collectors.groupingBy(uo -> uo.getUser().getId()));

        int count = ordersByUser.size();
        if (count == 0) return;

        BigDecimal deliveryPerUser = deliveryCost.divide(new BigDecimal(count), RoundingMode.HALF_UP);

        for (List<UserOrder> items : ordersByUser.values()) {
            boolean first = true;
            for (UserOrder item : items) {
                if (first) {
                    item.setDeliveryCost(deliveryPerUser);
                    first = false;
                } else {
                    item.setDeliveryCost(BigDecimal.ZERO);
                }
                userOrderRepo.save(item);
            }
        }
    }

    public OrderResponse mapToOrderResponse(Order order) {
        List<UserOrder> userOrders = userOrderRepo.findByOrder_IdOrderByIdAsc(order.getId());
        BigDecimal totalItems = userOrders.stream()
                .map(UserOrder::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Long, List<UserOrder>> byUser = userOrders.stream()
                .collect(Collectors.groupingBy(uo -> uo.getUser().getId()));

        BigDecimal deliveryTotal = byUser.values().stream()
                .map(list -> list.get(0).getDeliveryCost() != null ? list.get(0).getDeliveryCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return OrderResponse.fromEntity(order, totalItems.add(deliveryTotal), userOrders.size());
    }
}

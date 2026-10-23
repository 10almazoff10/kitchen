package ru.prokin.kitchen.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import ru.prokin.kitchen.dto.AddOrderItemRequest;
import ru.prokin.kitchen.dto.DishSuggestionDto;
import ru.prokin.kitchen.dto.OrderItemResponse;
import ru.prokin.kitchen.dto.UpdateOrderItemRequest;
import ru.prokin.kitchen.entity.Order;
import ru.prokin.kitchen.entity.User;
import ru.prokin.kitchen.entity.UserOrder;
import ru.prokin.kitchen.exception.BadRequestException;
import ru.prokin.kitchen.exception.ResourceNotFoundException;
import ru.prokin.kitchen.repository.OrderRepository;
import ru.prokin.kitchen.repository.UserOrderRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserOrderItemService {

    private final UserOrderRepository userOrderRepo;
    private final OrderRepository orderRepo;

    @Transactional
    public OrderItemResponse addItem(Long orderId, AddOrderItemRequest request, User user) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Заказ с ID " + orderId + " не найден"));

        if (order.isClosed()) {
            throw new BadRequestException("Нельзя добавлять позиции в закрытый заказ");
        }
        if (!order.isAcceptingOrders()) {
            throw new BadRequestException("Прием позиций в данный заказ приостановлен создателем");
        }

        UserOrder item = new UserOrder();
        item.setOrder(order);
        item.setUser(user);
        item.setItemDescription(request.getItemDescription());
        item.setPrice(request.getPrice());
        item.setDeliveryCost(BigDecimal.ZERO);
        item.setPaid(false);
        item.setAddedToRestaurantOrder(false);

        UserOrder saved = userOrderRepo.save(item);
        return OrderItemResponse.fromEntity(saved);
    }

    @Transactional
    public OrderItemResponse updateItem(Long itemId, UpdateOrderItemRequest request, User user) {
        UserOrder item = userOrderRepo.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Позиция с ID " + itemId + " не найдена"));

        if (!item.getUser().getId().equals(user.getId()) && !item.getOrder().getCreatedBy().getId().equals(user.getId())) {
            throw new BadRequestException("Вы не можете изменять чужую позицию");
        }

        if (item.getOrder().isClosed()) {
            throw new BadRequestException("Заказ уже закрыт");
        }

        item.setItemDescription(request.getItemDescription());
        item.setPrice(request.getPrice());
        UserOrder saved = userOrderRepo.save(item);
        return OrderItemResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteItem(Long itemId, User user) {
        UserOrder item = userOrderRepo.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Позиция с ID " + itemId + " не найдена"));

        if (!item.getUser().getId().equals(user.getId()) && !item.getOrder().getCreatedBy().getId().equals(user.getId())) {
            throw new BadRequestException("Вы не можете удалять чужую позицию");
        }

        if (item.getOrder().isClosed()) {
            throw new BadRequestException("Заказ уже закрыт");
        }

        userOrderRepo.delete(item);
    }

    @Transactional
    public OrderItemResponse togglePaid(Long itemId, User user) {
        UserOrder item = userOrderRepo.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Позиция с ID " + itemId + " не найдена"));

        // Отметить оплату может либо сам участник, либо создатель заказа
        if (!item.getUser().getId().equals(user.getId()) && !item.getOrder().getCreatedBy().getId().equals(user.getId())) {
            throw new BadRequestException("У вас нет прав изменять статус оплаты этой позиции");
        }

        item.setPaid(!item.isPaid());
        UserOrder saved = userOrderRepo.save(item);
        return OrderItemResponse.fromEntity(saved);
    }

    @Transactional
    public OrderItemResponse toggleAddedToOrder(Long itemId, User user) {
        UserOrder item = userOrderRepo.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Позиция с ID " + itemId + " не найдена"));

        // Отметить добавление в ресторанный заказ может создатель сбора
        if (!item.getOrder().getCreatedBy().getId().equals(user.getId())) {
            throw new BadRequestException("Только создатель заказа может отмечать позиции как добавленные в ресторанный чек");
        }

        item.setAddedToRestaurantOrder(!item.isAddedToRestaurantOrder());
        UserOrder saved = userOrderRepo.save(item);
        return OrderItemResponse.fromEntity(saved);
    }

    @Transactional
    public OrderItemResponse rateItem(Long itemId, Integer rating, User user) {
        UserOrder item = userOrderRepo.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Позиция с ID " + itemId + " не найдена"));

        if (!item.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Вы можете оценивать только свои позиции");
        }

        item.setRating(rating);
        UserOrder saved = userOrderRepo.save(item);
        return OrderItemResponse.fromEntity(saved);
    }

    @Transactional
    public void markUserItemsPaid(Long orderId, Long targetUserId, User currentUser) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Заказ с ID " + orderId + " не найден"));

        if (!order.getCreatedBy().getId().equals(currentUser.getId()) && !currentUser.getId().equals(targetUserId)) {
            throw new BadRequestException("Недостаточно прав для отметки оплаты");
        }

        List<UserOrder> items = userOrderRepo.findByOrder_IdAndUser_Id(orderId, targetUserId);
        items.forEach(i -> i.setPaid(true));
        userOrderRepo.saveAll(items);
    }

    @Transactional
    public void markUserItemsAdded(Long orderId, Long targetUserId, User currentUser) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Заказ с ID " + orderId + " не найден"));

        if (!order.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Только создатель заказа может отмечать позиции в чеке");
        }

        List<UserOrder> items = userOrderRepo.findByOrder_IdAndUser_Id(orderId, targetUserId);
        items.forEach(i -> i.setAddedToRestaurantOrder(true));
        userOrderRepo.saveAll(items);
    }

    @Transactional(readOnly = true)
    public ru.prokin.kitchen.dto.NotPaidOrdersResponse getMyUnpaidOrders(User user) {
        List<UserOrder> unpaid = userOrderRepo.findByUserAndIsPaidFalseOrderByIdDesc(user);
        List<OrderItemResponse> items = unpaid.stream()
                .map(OrderItemResponse::fromEntity)
                .toList();
        BigDecimal totalDebt = items.stream()
                .map(OrderItemResponse::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return ru.prokin.kitchen.dto.NotPaidOrdersResponse.builder()
                .items(items)
                .totalDebt(totalDebt)
                .totalCount(items.size())
                .build();
    }

    @Transactional(readOnly = true)
    public List<DishSuggestionDto> getPreviousDishes(Long restaurantId, User user) {
        if (user == null || restaurantId == null) {
            return Collections.emptyList();
        }
        List<UserOrder> recentOrders = userOrderRepo.findPreviousDishesByUserAndRestaurant(
                user, restaurantId, PageRequest.of(0, 100)
        );
        Map<String, DishSuggestionDto> distinct = new LinkedHashMap<>();
        for (UserOrder uo : recentOrders) {
            if (uo.getItemDescription() == null) continue;
            String desc = uo.getItemDescription().trim();
            if (desc.isEmpty()) continue;
            String key = desc.toLowerCase();
            if (!distinct.containsKey(key)) {
                distinct.put(key, new DishSuggestionDto(desc, uo.getPrice()));
                if (distinct.size() >= 20) {
                    break;
                }
            }
        }
        return new ArrayList<>(distinct.values());
    }

    @Transactional(readOnly = true)
    public List<DishSuggestionDto> getPreviousDishesForOrder(Long orderId, User user) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Заказ с ID " + orderId + " не найден"));
        if (order.getRestaurant() == null) {
            return Collections.emptyList();
        }
        return getPreviousDishes(order.getRestaurant().getId(), user);
    }
}

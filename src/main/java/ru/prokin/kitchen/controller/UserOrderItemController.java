package ru.prokin.kitchen.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.prokin.kitchen.dto.AddOrderItemRequest;
import ru.prokin.kitchen.dto.OrderItemResponse;
import ru.prokin.kitchen.dto.UpdateOrderItemRequest;
import ru.prokin.kitchen.entity.User;
import ru.prokin.kitchen.service.UserOrderItemService;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserOrderItemController {

    private final UserOrderItemService itemService;

    @PostMapping("/orders/{orderId}/items")
    public ResponseEntity<OrderItemResponse> addItem(
            @PathVariable Long orderId,
            @Valid @RequestBody AddOrderItemRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(itemService.addItem(orderId, request, user));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<OrderItemResponse> updateItem(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateOrderItemRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(itemService.updateItem(itemId, request, user));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable Long itemId,
            @AuthenticationPrincipal User user
    ) {
        itemService.deleteItem(itemId, user);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/items/{itemId}/toggle-paid")
    public ResponseEntity<OrderItemResponse> togglePaid(
            @PathVariable Long itemId,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(itemService.togglePaid(itemId, user));
    }

    @PatchMapping("/items/{itemId}/toggle-added")
    public ResponseEntity<OrderItemResponse> toggleAdded(
            @PathVariable Long itemId,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(itemService.toggleAddedToOrder(itemId, user));
    }

    @PostMapping("/items/{itemId}/rate")
    public ResponseEntity<OrderItemResponse> rateItem(
            @PathVariable Long itemId,
            @Valid @RequestBody ru.prokin.kitchen.dto.RateItemRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(itemService.rateItem(itemId, request.getRating(), user));
    }

    @PostMapping("/orders/{orderId}/users/{userId}/mark-paid")
    public ResponseEntity<Void> markUserPaid(
            @PathVariable Long orderId,
            @PathVariable Long userId,
            @AuthenticationPrincipal User user
    ) {
        itemService.markUserItemsPaid(orderId, userId, user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/orders/{orderId}/users/{userId}/mark-added")
    public ResponseEntity<Void> markUserAdded(
            @PathVariable Long orderId,
            @PathVariable Long userId,
            @AuthenticationPrincipal User user
    ) {
        itemService.markUserItemsAdded(orderId, userId, user);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/orders/unpaid")
    public ResponseEntity<ru.prokin.kitchen.dto.NotPaidOrdersResponse> getMyUnpaidOrders(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(itemService.getMyUnpaidOrders(user));
    }
}

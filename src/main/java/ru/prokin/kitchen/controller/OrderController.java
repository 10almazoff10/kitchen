package ru.prokin.kitchen.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.prokin.kitchen.dto.*;
import ru.prokin.kitchen.entity.User;
import ru.prokin.kitchen.service.OrderService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/active")
    public ResponseEntity<List<OrderResponse>> getActiveOrders() {
        return ResponseEntity.ok(orderService.getActiveOrders());
    }

    @GetMapping("/closed")
    public ResponseEntity<Page<OrderResponse>> getClosedOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("deadlineTime").descending());
        return ResponseEntity.ok(orderService.getClosedOrders(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailResponse> getOrderDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(orderService.getOrderDetail(id, user));
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(orderService.createOrder(request, user));
    }

    @PostMapping("/{id}/toggle-accepting")
    public ResponseEntity<Void> toggleAcceptingOrders(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        orderService.toggleAcceptingOrders(id, user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<Void> closeOrder(
            @PathVariable Long id,
            @Valid @RequestBody CloseOrderRequest request,
            @AuthenticationPrincipal User user
    ) {
        orderService.closeOrder(id, request.getDeliveryCost(), user);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/comment")
    public ResponseEntity<Void> updateComment(
            @PathVariable Long id,
            @RequestBody UpdateOrderCommentRequest request,
            @AuthenticationPrincipal User user
    ) {
        orderService.updateComment(id, request != null ? request.getComment() : null, user);
        return ResponseEntity.ok().build();
    }
}

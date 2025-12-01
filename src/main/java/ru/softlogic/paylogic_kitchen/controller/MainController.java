package ru.softlogic.paylogic_kitchen.controller;

import ru.softlogic.paylogic_kitchen.entity.Order;
import ru.softlogic.paylogic_kitchen.entity.User;
import ru.softlogic.paylogic_kitchen.entity.UserOrder;
import ru.softlogic.paylogic_kitchen.service.KitchenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class MainController {

    @Autowired private KitchenService kitchenService;

    @GetMapping("/")
    public String home(Authentication auth, Model model) {
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = auth.getName();
        User user = (User) kitchenService.loadUserByUsername(username);

        model.addAttribute("userName", user.getFullName()); // используем fullName
        model.addAttribute("activeOrders", kitchenService.getActiveOrders());
        model.addAttribute("closedOrders", kitchenService.getClosedOrders());
        return "index";
    }
    @GetMapping("/create_order")
    public String createOrderPage() {
        return "create_order";
    }

    @PostMapping("/stop-accepting/{id}")
    public String stopAccepting(@PathVariable Long id, Authentication auth) {
        // Проверка, что пользователь — создатель заказа (опционально)
        kitchenService.stopAcceptingOrders(id);
        return "redirect:/";
    }

    @PostMapping("/create-order")
    public String createOrder(@RequestParam String restaurantUrl,
                              @RequestParam String deadline,
                              @RequestParam String paymentData,
                              Authentication auth) {
        String username = auth.getName();
        User user = (User) kitchenService.loadUserByUsername(username);
        java.time.LocalDateTime deadlineTime = java.time.LocalDateTime.parse(deadline);
        kitchenService.createOrder(restaurantUrl, deadlineTime, user, paymentData);
        return "redirect:/";
    }

    @PostMapping("/close-order/{id}")
    public String closeOrder(@PathVariable Long id, Authentication auth) {
        // Проверка, что пользователь — создатель заказа (опционально)
        kitchenService.closeOrder(id);
        return "redirect:/";
    }

    @PostMapping("/add-item/{orderId}")
    public String addItem(@PathVariable Long orderId,
                          @RequestParam String item,
                          @RequestParam(defaultValue = "0") String price,
                          Authentication auth) {

        Order order = kitchenService.getOrderById(orderId);
        if (order.isAcceptingOrders() && !order.isClosed()) {
            String username = auth.getName();
            User user = (User) kitchenService.loadUserByUsername(username);
            java.math.BigDecimal priceNum = new java.math.BigDecimal(price);
            kitchenService.addUserOrder(orderId, user.getId(), item, priceNum);
            return "redirect:/order/" + orderId;
        }else{
            return "redirect:/order/" + orderId;
        }
    }

    @GetMapping("/order/{id}")
    public String viewOrder(@PathVariable Long id, Model model, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }

        Order order = kitchenService.getOrderById(id);
        List<UserOrder> orderItems = kitchenService.getUsersItemsInOrder(id);
        BigDecimal totalAmount = kitchenService.getTotalAmountForOrder(id);

        model.addAttribute("order", order);
        model.addAttribute("orderItems", orderItems);
        model.addAttribute("totalAmount", totalAmount);
        return "order_detail";
    }

    @PostMapping("/mark-added/{userOrderId}")
    public String markAdded(@PathVariable Long userOrderId, Authentication auth) {
        kitchenService.markUserOrderAsAdded(userOrderId);
        UserOrder userOrder = kitchenService.getUserOrderByUserOrderId(userOrderId);
        Long orderId = userOrder.getOrder().getId();
        return "redirect:/order/" + orderId;
    }

    @PostMapping("/mark-paid/{userOrderId}")
    public String markPaid(@PathVariable Long userOrderId, Authentication auth) {
        kitchenService.markUserOrderAsPaid(userOrderId);
        UserOrder userOrder = kitchenService.getUserOrderByUserOrderId(userOrderId);
        Long orderId = userOrder.getOrder().getId();
        return "redirect:/order/" + orderId;
    }

    @PostMapping("/edit-item/{userOrderId}")
    public String editItem(@PathVariable Long userOrderId,
                           @RequestParam String item,
                           @RequestParam String price,
                           Authentication auth) {
        BigDecimal priceNum = new BigDecimal(price);
        kitchenService.updateUserOrder(userOrderId, item, priceNum);
        UserOrder userOrder = kitchenService.getUserOrderByUserOrderId(userOrderId);
        Long orderId = userOrder.getOrder().getId();
        return "redirect:/order/" + orderId;
    }
}
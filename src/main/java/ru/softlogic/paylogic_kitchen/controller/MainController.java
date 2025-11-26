package ru.softlogic.paylogic_kitchen.controller;


import ru.softlogic.paylogic_kitchen.entity.Order;
import ru.softlogic.paylogic_kitchen.entity.User;
import ru.softlogic.paylogic_kitchen.entity.UserOrder;
import ru.softlogic.paylogic_kitchen.service.KitchenService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class MainController {

    @Autowired private KitchenService kitchenService;

    @GetMapping("/")
    public String home(HttpSession session, Model model) {
        String userName = (String) session.getAttribute("userName");
        if (userName == null) {
            return "redirect:/login";
        }

        model.addAttribute("userName", userName);
        model.addAttribute("activeOrders", kitchenService.getActiveOrders());
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login_form";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String name, HttpSession session) {
        User user = kitchenService.findOrCreateUser(name);
        session.setAttribute("userName", user.getName());
        session.setAttribute("userId", user.getId());
        return "redirect:/";
    }

    @PostMapping("/create-order")
    public String createOrder(@RequestParam String restaurantUrl,
                              @RequestParam String deadline,
                              @RequestParam String paymentData, // добавлено
                              HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        User user = kitchenService.findOrCreateUser(kitchenService.findOrCreateUser((String) session.getAttribute("userName")).getName());
        java.time.LocalDateTime deadlineTime = java.time.LocalDateTime.parse(deadline);
        kitchenService.createOrder(restaurantUrl, deadlineTime, user, paymentData); // передаём paymentData
        return "redirect:/";
    }

    @PostMapping("/close-order/{id}")
    public String closeOrder(@PathVariable Long id) {
        kitchenService.closeOrder(id);
        return "redirect:/";
    }

    @PostMapping("/add-item/{orderId}")
    public String addItem(@PathVariable Long orderId,
                          @RequestParam String item,
                          @RequestParam(defaultValue = "0") String price,
                          HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        java.math.BigDecimal priceNum = new java.math.BigDecimal(price);
        kitchenService.addUserOrder(orderId, userId, item, priceNum);
        return "redirect:/order/" + orderId;
    }

    @GetMapping("/order/{id}")
    public String viewOrder(@PathVariable Long id, Model model) {
        Order order = kitchenService.getActiveOrders().stream()
                .filter(o -> o.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Order not found or closed"));

        List<UserOrder> orderItems = kitchenService.getUsersItemsInOrder(id);

        // Добавляем подсчёт общей суммы
        BigDecimal totalAmount = kitchenService.getTotalAmountForOrder(id);

        model.addAttribute("order", order);
        model.addAttribute("orderItems", orderItems);
        model.addAttribute("totalAmount", totalAmount); // <-- добавлено
        return "order_detail";
    }

    @PostMapping("/mark-added/{userOrderId}")
    public String markAdded(@PathVariable Long userOrderId) {
        kitchenService.markUserOrderAsAdded(userOrderId);
        // Получаем orderId для редиректа обратно на страницу заказа
        UserOrder userOrder = kitchenService.getUserOrderByUserOrderId(userOrderId);
        Long orderId = userOrder.getOrder().getId();
        return "redirect:/order/" + orderId;
    }

    @PostMapping("/mark-paid/{userOrderId}")
    public String markPaid(@PathVariable Long userOrderId) {
        kitchenService.markUserOrderAsPaid(userOrderId);
        UserOrder userOrder = kitchenService.getUserOrderByUserOrderId(userOrderId);
        Long orderId = userOrder.getOrder().getId();
        return "redirect:/order/" + orderId;
    }
}
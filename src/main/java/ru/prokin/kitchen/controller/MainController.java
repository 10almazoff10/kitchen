package ru.prokin.kitchen.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Value;
import ru.prokin.kitchen.dto.UserOrderSummary;
import ru.prokin.kitchen.entity.Order;
import ru.prokin.kitchen.entity.Restaurant;
import ru.prokin.kitchen.entity.User;
import ru.prokin.kitchen.entity.UserOrder;
import ru.prokin.kitchen.service.KitchenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.prokin.kitchen.service.UserService;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class MainController {

    @Autowired private KitchenService kitchenService;
    @Autowired private UserService userService;


    @Value("${app.version}")
    private String appVersion;

    @Value("${app.headerName}")
    private String headerName;

    @GetMapping("/")
    public String home(Authentication auth,
                       Model model,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "5") int size) {
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = auth.getName();
        User user = (User) kitchenService.loadUserByUsername(username);
        Pageable pageable = PageRequest.of(page, size, Sort.by("deadlineTime").descending());

        model.addAttribute("userName", user.getFullName());
        model.addAttribute("activeOrders", kitchenService.getActiveOrders());
        model.addAttribute("closedOrders", kitchenService.getClosedOrders(pageable));
        model.addAttribute("currentUserId", user.getId());
        model.addAttribute("appVersion", appVersion);
        model.addAttribute("title", "Главная");
        model.addAttribute("headerName", headerName);

        return "index";
    }

    @GetMapping("/create-order")
    public String showCreateOrderForm(Model model, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }

        String username = auth.getName();
        User currentUser = (User) userService.loadUserByUsername(username);

        model.addAttribute("userName", currentUser.getFullName());
        model.addAttribute("currentUserId", currentUser.getId());
        model.addAttribute("restaurants", kitchenService.getAllRestaurants());
        model.addAttribute("appVersion", appVersion);
        model.addAttribute("title", "Создание заказа");
        model.addAttribute("headerName", headerName);


        return "create_order";
    }

    @PostMapping("/stop-accepting/{id}")
    public String stopAccepting(@PathVariable Long id, Authentication auth) {
        // Проверка, что пользователь — создатель заказа (опционально)
        kitchenService.stopAcceptingOrders(id);
        return "redirect:/order/{id}";
    }

    @PostMapping("/create-order")
    public String createOrder(@RequestParam Long restaurantId,
                              @RequestParam String deadline,
                              @RequestParam String paymentData,
                              @RequestParam String comment,
                              Authentication auth) {
        String username = auth.getName();
        User user = (User) userService.loadUserByUsername(username);

        Restaurant restaurant = kitchenService.getRestaurantById(restaurantId);
        java.time.LocalDateTime deadlineTime = java.time.LocalDateTime.parse(deadline);

        kitchenService.createOrder(restaurant, deadlineTime, user, paymentData, comment);

        return "redirect:/";
    }

    @GetMapping("/add-restaurant")
    public String showAddRestaurantForm(Model model, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }

        model.addAttribute("title", "Добавление нового ресторана");
        model.addAttribute("headerName", headerName);

        return "add_restaurant_form";
    }

    @PostMapping("/add-restaurant")
    public String addRestaurant(@RequestParam String name, @RequestParam String websiteUrl, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }

        Restaurant restaurant = new Restaurant();
        restaurant.setName(name);
        restaurant.setWebsiteUrl(websiteUrl);
        kitchenService.saveRestaurant(restaurant);
        return "redirect:/create-order";
    }

    @PostMapping("/close-order/{id}")
    public String closeOrder(@PathVariable Long id,
                             @RequestParam(required = false, defaultValue = "0") String deliveryCost,
                             Authentication auth) {
        // Проверка, что пользователь — создатель заказа (опционально)
        java.math.BigDecimal deliveryCostNum = new java.math.BigDecimal(deliveryCost);
        kitchenService.closeOrder(id, deliveryCostNum);
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
    @PostMapping("/delete-item/{userOrderId}")
    public String deleteItem(@PathVariable Long userOrderId, Authentication auth) {
        UserOrder userOrder = kitchenService.getUserOrderByUserOrderId(userOrderId); // получаем до удаления
        Long orderId = userOrder.getOrder().getId();
        kitchenService.deleteUserOrder(userOrderId); // теперь удаляем
        return "redirect:/order/" + orderId;
    }

    @GetMapping("/order/{id}")
    public String viewOrder(@PathVariable Long id, Model model, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }

        Order order = kitchenService.getOrderById(id);
        List<UserOrder> orderItems = kitchenService.getUsersItemsInOrder(id);
        List<UserOrderSummary> userSummaries = kitchenService.getUserOrderSummariesByOrderId(id);
        BigDecimal totalAmount = kitchenService.getTotalAmountForOrder(id);

        String username = auth.getName();
        User user = (User) kitchenService.loadUserByUsername(username);

        model.addAttribute("order", order);
        model.addAttribute("orderItems", orderItems);
        model.addAttribute("userSummaries", userSummaries);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("appVersion", appVersion);
        model.addAttribute("currentUserId", user.getId());
        model.addAttribute("title", "Заказ №" + order.getId());
        model.addAttribute("headerName", headerName);


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

    @PostMapping("/rate-item/{userOrderId}")
    public String rateItem(@PathVariable Long userOrderId,
                           @RequestParam Integer rating,
                           Authentication auth) {
        String username = auth.getName();
        User user = (User) userService.loadUserByUsername(username);

        kitchenService.updateRating(userOrderId, rating, user.getId());

        // Найдём orderId, чтобы вернуться к заказу
        UserOrder userOrder = kitchenService.getUserOrderByUserOrderId(userOrderId);
        Long orderId = userOrder.getOrder().getId();

        return "redirect:/order/" + orderId;
    }

    @PostMapping("/mark-paid-user/{userId}")
    public String markAllPaid(@PathVariable Long userId, @RequestParam Long orderId) {
        kitchenService.markAllItemsPaidByUser(orderId, userId);
        return "redirect:/order/" + orderId;
    }

    @PostMapping("/mark-added-user/{userId}")
    public String markAllAdded(@PathVariable Long userId, @RequestParam Long orderId) {
        kitchenService.markAllItemsAddedByUser(orderId, userId);
        return "redirect:/order/" + orderId;
    }
}
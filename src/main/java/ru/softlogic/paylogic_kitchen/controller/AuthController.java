package ru.softlogic.paylogic_kitchen.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.softlogic.paylogic_kitchen.service.UserService;

@Controller
public class AuthController {

    @Autowired private UserService userService;

    @GetMapping("/register")
    public String showRegisterForm() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam String username,
                               @RequestParam String password,
                               @RequestParam String fullName,
                               Model model) {
        try {
            userService.createUser(username, password, fullName);
            model.addAttribute("success", true);
            return "login";
        } catch (Exception e) {
            model.addAttribute("error", "Error registering user: " + e.getMessage());
            return "register";
        }
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }
}
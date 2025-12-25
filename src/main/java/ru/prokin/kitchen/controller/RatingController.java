package ru.prokin.kitchen.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import ru.prokin.kitchen.service.RatingService;

@Controller
public class RatingController {

    @Value("${app.headerName}")
    private String headerName;

    @Value("${app.version}")
    private String appVersion;

    @Autowired
    private RatingService ratingService;

    @GetMapping("/rating")
    public String showRating(Model model, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }

        model.addAttribute("topSpentThisMonth", ratingService.getTopSpentThisMonth());
        model.addAttribute("topOrdersThisMonth", ratingService.getTopOrdersThisMonth());
        model.addAttribute("topSpentAllTime", ratingService.getTopSpentAllTime());
        model.addAttribute("topOrdersAllTime", ratingService.getTopOrdersAllTime());
        model.addAttribute("appVersion", appVersion);
        model.addAttribute("title", "Рейтинг пользователей");
        model.addAttribute("headerName", headerName);

        return "rating";
    }
}
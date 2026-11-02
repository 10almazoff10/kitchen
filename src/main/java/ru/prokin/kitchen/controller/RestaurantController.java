package ru.prokin.kitchen.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.prokin.kitchen.dto.ParseMenuRequest;
import ru.prokin.kitchen.dto.ParsedMenuItemDto;
import ru.prokin.kitchen.dto.RestaurantRequest;
import ru.prokin.kitchen.dto.RestaurantResponse;
import ru.prokin.kitchen.entity.Restaurant;
import ru.prokin.kitchen.service.MenuParserService;
import ru.prokin.kitchen.service.RestaurantService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;
    private final MenuParserService menuParserService;

    @GetMapping
    public ResponseEntity<List<RestaurantResponse>> getAllRestaurants() {
        List<RestaurantResponse> list = restaurantService.getAllRestaurants().stream()
                .map(r -> RestaurantResponse.fromEntity(r, menuParserService.hasProvider(r.getWebsiteUrl(), r.getName())))
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestaurantResponse> getRestaurantById(@PathVariable Long id) {
        Restaurant r = restaurantService.getRestaurantById(id);
        boolean hasMenu = r != null && menuParserService.hasProvider(r.getWebsiteUrl(), r.getName());
        return ResponseEntity.ok(RestaurantResponse.fromEntity(r, hasMenu));
    }

    @PostMapping
    public ResponseEntity<RestaurantResponse> createRestaurant(@Valid @RequestBody RestaurantRequest request) {
        Restaurant created = restaurantService.createRestaurant(request.getName(), request.getWebsiteUrl());
        boolean hasMenu = menuParserService.hasProvider(created.getWebsiteUrl(), created.getName());
        return ResponseEntity.ok(RestaurantResponse.fromEntity(created, hasMenu));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RestaurantResponse> updateRestaurant(
            @PathVariable Long id,
            @Valid @RequestBody RestaurantRequest request
    ) {
        Restaurant updated = restaurantService.updateRestaurant(id, request.getName(), request.getWebsiteUrl());
        boolean hasMenu = menuParserService.hasProvider(updated.getWebsiteUrl(), updated.getName());
        return ResponseEntity.ok(RestaurantResponse.fromEntity(updated, hasMenu));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRestaurant(@PathVariable Long id) {
        restaurantService.deleteRestaurant(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/parse-menu")
    public ResponseEntity<List<ParsedMenuItemDto>> parseMenu(@Valid @RequestBody ParseMenuRequest request) {
        return ResponseEntity.ok(menuParserService.parseMenu(request.getUrl()));
    }

    @GetMapping("/{id}/menu")
    public ResponseEntity<List<ParsedMenuItemDto>> getRestaurantMenu(@PathVariable Long id) {
        Restaurant restaurant = restaurantService.getRestaurantById(id);
        if (restaurant == null || !org.springframework.util.StringUtils.hasText(restaurant.getWebsiteUrl())) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(menuParserService.parseMenu(restaurant.getWebsiteUrl()));
    }
}

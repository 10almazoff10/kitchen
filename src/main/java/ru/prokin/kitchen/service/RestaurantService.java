package ru.prokin.kitchen.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.prokin.kitchen.entity.Restaurant;
import ru.prokin.kitchen.exception.ResourceNotFoundException;
import ru.prokin.kitchen.repository.RestaurantRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepo;

    public List<Restaurant> getAllRestaurants() {
        return restaurantRepo.findAll();
    }

    public Restaurant getRestaurantById(Long id) {
        return restaurantRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ресторан с ID " + id + " не найден"));
    }

    @Transactional
    public Restaurant createRestaurant(String name, String websiteUrl) {
        Restaurant restaurant = new Restaurant();
        restaurant.setName(name);
        restaurant.setWebsiteUrl(websiteUrl);
        return restaurantRepo.save(restaurant);
    }

    @Transactional
    public Restaurant updateRestaurant(Long id, String name, String websiteUrl) {
        Restaurant restaurant = getRestaurantById(id);
        restaurant.setName(name);
        restaurant.setWebsiteUrl(websiteUrl);
        return restaurantRepo.save(restaurant);
    }

    @Transactional
    public void deleteRestaurant(Long id) {
        Restaurant restaurant = getRestaurantById(id);
        restaurantRepo.delete(restaurant);
    }
}

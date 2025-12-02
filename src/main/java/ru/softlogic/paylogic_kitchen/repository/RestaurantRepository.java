package ru.softlogic.paylogic_kitchen.repository;

import ru.softlogic.paylogic_kitchen.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    List<Restaurant> findAll();
}
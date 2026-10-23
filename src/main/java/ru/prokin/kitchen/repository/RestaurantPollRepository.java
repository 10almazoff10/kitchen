package ru.prokin.kitchen.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.prokin.kitchen.entity.RestaurantPoll;

import java.time.LocalDate;
import java.util.Optional;

public interface RestaurantPollRepository extends JpaRepository<RestaurantPoll, Long> {
    Optional<RestaurantPoll> findByPollDate(LocalDate pollDate);
}

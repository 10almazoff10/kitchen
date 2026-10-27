package ru.prokin.kitchen.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.prokin.kitchen.entity.PollVote;
import ru.prokin.kitchen.entity.Restaurant;
import ru.prokin.kitchen.entity.RestaurantPoll;
import ru.prokin.kitchen.entity.User;

import java.util.List;
import java.util.Optional;

public interface PollVoteRepository extends JpaRepository<PollVote, Long> {
    Optional<PollVote> findByPollAndUserAndRestaurant(RestaurantPoll poll, User user, Restaurant restaurant);

    List<PollVote> findByPollAndUser(RestaurantPoll poll, User user);

    @Query("SELECT pv FROM PollVote pv JOIN FETCH pv.user JOIN FETCH pv.restaurant WHERE pv.poll = :poll")
    List<PollVote> findAllWithUserAndRestaurantByPoll(@Param("poll") RestaurantPoll poll);

    void deleteByPollAndUser(RestaurantPoll poll, User user);

    void deleteByPollAndUserAndRestaurant(RestaurantPoll poll, User user, Restaurant restaurant);

    long countByPoll(RestaurantPoll poll);
}

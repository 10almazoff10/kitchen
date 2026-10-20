package ru.prokin.kitchen.repository;

import ru.prokin.kitchen.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByAdUsername(String adUsername);
    Optional<User> findByLegacyUsername(String legacyUsername);

    @Query("SELECT u FROM User u WHERE LOWER(u.username) = LOWER(:login) OR (u.adUsername IS NOT NULL AND LOWER(u.adUsername) = LOWER(:login)) OR (u.legacyUsername IS NOT NULL AND LOWER(u.legacyUsername) = LOWER(:login))")
    Optional<User> findByAnyUsername(@Param("login") String login);
}
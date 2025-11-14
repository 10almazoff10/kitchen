package ru.softlogic.paylogic_kitchen.repository;

import ru.softlogic.paylogic_kitchen.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByName(String name);
}
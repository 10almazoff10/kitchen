package ru.softlogic.paylogic_kitchen.repository;

import ru.softlogic.paylogic_kitchen.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByIsClosedFalse();
    List<Order> findByIsClosedFalseOrderByIdDesc();
    List<Order> findByIsClosedTrueOrderByIdDesc();
}
package ru.softlogic.paylogic_kitchen.repository;

import ru.softlogic.paylogic_kitchen.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByIsClosedFalse();
    List<Order> findByIsClosedFalseOrderByIdDesc();
    Page<Order> findByIsClosedTrueOrderByIdDesc(Pageable pageable);  // закрытые заказы
}
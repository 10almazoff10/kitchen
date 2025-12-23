package ru.softlogic.paylogic_kitchen.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.softlogic.paylogic_kitchen.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByIsClosedFalse();
    List<Order> findByIsClosedFalseOrderByIdDesc();
    Page<Order> findByIsClosedTrueOrderByIdDesc(Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.deadlineTime <= :time AND o.isClosed = false")
    List<Order> findOrdersByDeadlineBefore(@Param("time") LocalDateTime time);

    @Query("SELECT o FROM Order o WHERE o.deadlineTime <= :time AND o.deadlineTime > :timeMinus5Min AND o.deadlineWarningSent = false")
    List<Order> findOrdersForWarning(@Param("time") LocalDateTime time, @Param("timeMinus5Min") LocalDateTime timeMinus5Min);

    @Query("SELECT o FROM Order o WHERE o.deadlineTime <= :time AND o.deadlineTime > :timeMinus1Min AND o.deadlineNotified = false")
    List<Order> findOrdersForNotification(@Param("time") LocalDateTime time, @Param("timeMinus1Min") LocalDateTime timeMinus1Min);
}
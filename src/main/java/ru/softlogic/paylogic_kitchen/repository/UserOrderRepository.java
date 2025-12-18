package ru.softlogic.paylogic_kitchen.repository;

import org.springframework.data.repository.query.Param;
import ru.softlogic.paylogic_kitchen.entity.Order;
import ru.softlogic.paylogic_kitchen.entity.UserOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface UserOrderRepository extends JpaRepository<UserOrder, Long> {
    List<UserOrder> getUserOrdersByOrder(Order order);

    // Добавь этот метод:
    List<UserOrder> findByOrder_Id(Long orderId);

    @Query("SELECT uo FROM UserOrder uo JOIN FETCH uo.user WHERE uo.order.id = :orderId ORDER BY uo.id ASC")
    List<UserOrder> findWithUserByOrder_Id(@Param("orderId") Long orderId);

    // 1. Сумма потраченных денег пользователем за период
    @Query("SELECT uo.user, SUM(uo.price) FROM UserOrder uo WHERE uo.isPaid = true AND uo.createdDate >= :startOfMonth GROUP BY uo.user ORDER BY SUM(uo.price) DESC")
    List<Object[]> findTopSpentByUserInPeriod(@Param("startOfMonth") LocalDateTime startOfMonth);

    // 2. Количество заказов пользователем за период
    @Query("SELECT uo.user, COUNT(uo) FROM UserOrder uo WHERE uo.createdDate >= :startOfMonth GROUP BY uo.user ORDER BY COUNT(uo) DESC")
    List<Object[]> findTopOrdersByUserInPeriod(@Param("startOfMonth") LocalDateTime startOfMonth);

    // 3. Сумма потраченных денег пользователем за всё время
    @Query("SELECT uo.user, SUM(uo.price) FROM UserOrder uo WHERE uo.isPaid = true GROUP BY uo.user ORDER BY SUM(uo.price) DESC")
    List<Object[]> findTopSpentByUserAllTime();

    // 4. Количество заказов пользователем за всё время
    @Query("SELECT uo.user, COUNT(uo) FROM UserOrder uo GROUP BY uo.user ORDER BY COUNT(uo) DESC")
    List<Object[]> findTopOrdersByUserAllTime();
}

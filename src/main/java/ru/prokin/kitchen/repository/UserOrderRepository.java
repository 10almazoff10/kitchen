package ru.prokin.kitchen.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import ru.prokin.kitchen.entity.Order;
import ru.prokin.kitchen.entity.UserOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface UserOrderRepository extends JpaRepository<UserOrder, Long> {
    List<UserOrder> getUserOrdersByOrder(Order order);

    List<UserOrder> findByOrder_IdOrderByIdAsc(Long orderId);

    @Query("SELECT uo FROM UserOrder uo JOIN FETCH uo.user WHERE uo.order.id = :orderId ORDER BY uo.id ASC")
    List<UserOrder> findWithUserByOrder_Id(@Param("orderId") Long orderId);

    @Query("SELECT uo FROM UserOrder uo WHERE uo.order.id = :orderId AND uo.user.id = :userId")
    List<UserOrder> findByOrder_IdAndUser_Id(@Param("orderId") Long orderId, @Param("userId") Long userId);

    // 1. Сумма потраченных денег пользователем за период
    @Query("SELECT uo.user, SUM(uo.price) FROM UserOrder uo WHERE uo.isPaid = true AND uo.createdDate >= :startOfMonth GROUP BY uo.user ORDER BY SUM(uo.price) DESC")
    List<Object[]> findTopSpentByUserInPeriod(@Param("startOfMonth") LocalDateTime startOfMonth);

    // 2. Количество заказов, в которых участвовал пользователь за период
    @Query("SELECT uo.user, COUNT(DISTINCT uo.order) FROM UserOrder uo JOIN uo.user WHERE uo.createdDate >= :startOfMonth GROUP BY uo.user ORDER BY COUNT(DISTINCT uo.order) DESC")
    List<Object[]> findTopOrdersByUserInPeriod(@Param("startOfMonth") LocalDateTime startOfMonth);

    // 3. Сумма потраченных денег пользователем за всё время
    @Query("SELECT uo.user, SUM(uo.price) FROM UserOrder uo WHERE uo.isPaid = true GROUP BY uo.user ORDER BY SUM(uo.price) DESC")
    List<Object[]> findTopSpentByUserAllTime();

    // 4. Количество заказов, в которых участвовал пользователь за всё время
    @Query("SELECT uo.user, COUNT(DISTINCT uo.order) FROM UserOrder uo JOIN uo.user GROUP BY uo.user ORDER BY COUNT(DISTINCT uo.order) DESC")
    List<Object[]> findTopOrdersByUserAllTime();

    @Modifying
    @Query("UPDATE UserOrder uo SET uo.rating = :rating WHERE uo.id = :userOrderId")
    void updateRating(@Param("userOrderId") Long userOrderId, @Param("rating") Integer rating);

}

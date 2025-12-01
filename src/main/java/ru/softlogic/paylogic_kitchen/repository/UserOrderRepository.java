package ru.softlogic.paylogic_kitchen.repository;

import ru.softlogic.paylogic_kitchen.entity.Order;
import ru.softlogic.paylogic_kitchen.entity.UserOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface UserOrderRepository extends JpaRepository<UserOrder, Long> {
    List<UserOrder> getUserOrdersByOrder(Order order);

    // Добавь этот метод:
    List<UserOrder> findByOrder_Id(Long orderId);

    @Query("SELECT uo FROM UserOrder uo JOIN FETCH uo.user WHERE uo.order.id = :orderId ORDER BY uo.id ASC")
    List<UserOrder> findWithUserByOrder_Id(Long orderId);
}
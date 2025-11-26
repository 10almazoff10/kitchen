package ru.softlogic.paylogic_kitchen.repository;


import ru.softlogic.paylogic_kitchen.entity.Order;
import ru.softlogic.paylogic_kitchen.entity.UserOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;


public interface UserOrderRepository extends JpaRepository<UserOrder, Long> {
    List<UserOrder> getUserOrdersByOrder(Order order);
}
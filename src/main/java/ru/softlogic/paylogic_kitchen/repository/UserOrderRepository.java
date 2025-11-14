package ru.softlogic.paylogic_kitchen.repository;


import ru.softlogic.paylogic_kitchen.entity.UserOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserOrderRepository extends JpaRepository<UserOrder, Long> {}
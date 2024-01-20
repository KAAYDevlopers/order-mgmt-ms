package com.abw12.absolutefitness.ordermgmtms.repository;

import com.abw12.absolutefitness.ordermgmtms.entity.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItemEntity,String> {
    @Query("SELECT o FROM OrderItemEntity o WHERE o.orderId=:orderId")
    Optional<List<OrderItemEntity>> fetchOrderItemsByOderId(String orderId);
}

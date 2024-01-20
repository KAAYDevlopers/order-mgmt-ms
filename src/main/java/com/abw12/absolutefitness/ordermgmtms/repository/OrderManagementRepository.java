package com.abw12.absolutefitness.ordermgmtms.repository;

import com.abw12.absolutefitness.ordermgmtms.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderManagementRepository extends JpaRepository<OrderEntity,String> {

    @Query("SELECT o FROM OrderEntity o WHERE o.orderId=:orderId")
    Optional<OrderEntity> getOrderById(String orderId);

    @Query("SELECT o FROM OrderEntity o WHERE o.pgOrderId=:pgOrderId")
    Optional<OrderEntity> getOrderByPgOrderId(String pgOrderId);
}

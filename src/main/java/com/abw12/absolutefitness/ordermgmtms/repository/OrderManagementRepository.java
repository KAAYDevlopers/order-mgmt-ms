package com.abw12.absolutefitness.ordermgmtms.repository;

import com.abw12.absolutefitness.ordermgmtms.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderManagementRepository extends JpaRepository<OrderEntity,String> {
}

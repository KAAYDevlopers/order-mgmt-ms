package com.abw12.absolutefitness.ordermgmtms.repository;

import com.abw12.absolutefitness.ordermgmtms.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentEntityRepository extends JpaRepository<PaymentEntity,String> {

    @Query("SELECT p FROM PaymentEntity p WHERE p.orderId=:orderId")
    Optional<PaymentEntity> getPaymentEntityByOrderId(String orderId);
}

package com.abw12.absolutefitness.ordermgmtms.repository;

import com.abw12.absolutefitness.ordermgmtms.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentEntityRepository extends JpaRepository<PaymentEntity,String> {
}

package com.abw12.absolutefitness.ordermgmtms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "paymentdetails",schema = "ordermgmt")
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payment_id")
    private String paymentId;
    @Column(name = "pg_payment_id")
    private String pgPaymentId;
    @Column(name = "order_id")
    private String orderId; //internal orderId(foreign key to orderEntity)
    @Column(name = "razorpay_signature")
    private String razorpaySignature;
    @Column(name = "payment_status")
    private String paymentStatus;
    @Column(name = "payment_method")
    private String paymentMethod;
    @Column(name = "payment_amount")
    private BigDecimal amount;
    @Column(name = "payment_date")
    private OffsetDateTime paymentDate;
}

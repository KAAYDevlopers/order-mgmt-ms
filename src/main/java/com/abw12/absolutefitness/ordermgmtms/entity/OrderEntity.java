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
@Table(name = "orders",schema = "ordermgmt")
public class OrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id")
    private String orderId; // (primary key)
    @Column(name = "pg_order_id")
    private String pgOrderId;
    @Column(name = "user_id")
    private String userId;
    @Column(name = "order_number")
    private Long orderNumber;
    @Column(name = "order_status")
    private String orderStatus; //(e.g., PENDING, COMPLETED, FAILED)
    @Column(name = "total_amount")
    private BigDecimal totalAmount;
    private String currency;
    @Column(name = "payment_id")
    private String paymentId;//(foreign key to PaymentEntity)
    @Column(name = "payment_signature_verification")
    private Boolean paymentSignatureVerification;
    @Column(name = "shipping_address")
    private String shippingAddress;
    @Column(name = "billing_address")
    private String billingAddress;
    @Column(name = "order_placed_date")
    private OffsetDateTime orderPlacedDate;
    @Column(name="order_created_at")
    private OffsetDateTime orderCreatedAt;
    @Column(name="order_Modified_at")
    private OffsetDateTime orderModifiedAt;
}

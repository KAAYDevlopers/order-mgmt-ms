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
    private String paymentId;   //internal paymentId which is also a primary key
    @Column(name = "pg_payment_id")
    private String pgPaymentId;
    @Column(name = "order_id")
    private String orderId; //internal orderId(foreign key to orderEntity)
    @Column(name = "payment_status")
    private String paymentStatus;
    @Column(name = "payment_method")
    private String paymentMethod;
    @Column(name = "payment_amount")
    private BigDecimal paymentAmount;
    @Column(name = "invoice_id")
    private String invoiceId;
    @Column(name = "refund_status")
    private String refundStatus;
    @Column(name = "amount_refunded")
    private Integer amountRefunded;
    @Column(name = "payment_created_date")
    private OffsetDateTime paymentCreatedDate;
}

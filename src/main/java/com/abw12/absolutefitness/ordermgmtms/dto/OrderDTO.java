package com.abw12.absolutefitness.ordermgmtms.dto;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderDTO {

    @Id
    private String orderId; // (primary key)
    private String pgOrderId;
    private String userId;
    private Long orderNumber;
    private String orderStatus; //(e.g., PENDING, COMPLETED, FAILED)
    private BigDecimal totalAmount;
    private String currency;
    private String paymentId;//(foreign key to PaymentEntity)
    private Boolean paymentSignatureVerification;
    private String shippingAddress;
    private String billingAddress;
    private List<OrderItemDTO> orderItems;
    private String orderCreatedAt;
    private String orderModifiedAt;

}

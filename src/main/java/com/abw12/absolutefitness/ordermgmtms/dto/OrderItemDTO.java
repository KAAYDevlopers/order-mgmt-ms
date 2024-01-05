package com.abw12.absolutefitness.ordermgmtms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemDTO {
    private String orderItemId; //(primary key)
    private String orderId; //(foreign key to OrderEntity)
    private String productId;
    private String variantId;
    private Long quantity;
    private BigDecimal pricePerUnit;
    private OffsetDateTime orderItemCreatedAt;
}

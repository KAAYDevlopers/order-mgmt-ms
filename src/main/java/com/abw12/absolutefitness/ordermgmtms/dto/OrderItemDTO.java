package com.abw12.absolutefitness.ordermgmtms.dto;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemDTO {
    @Id
    private String orderItemId; //(primary key)
    private String orderId; //(foreign key to OrderEntity)
    private String productId;
    private String variantId;
    private String variantInventoryId;
    private Long quantity;
    private BigDecimal pricePerUnit;
    private String orderItemCreatedAt;
}

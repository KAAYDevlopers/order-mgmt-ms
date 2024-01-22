package com.abw12.absolutefitness.ordermgmtms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemHistory {

    private String productName;
    private String variantName;
    private String variantValue;
    private String variantType;
    private String imagePath;
    private Long quantity;
    private BigDecimal pricePerUnit;
}

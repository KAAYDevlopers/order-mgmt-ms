package com.abw12.absolutefitness.ordermgmtms.dto;

import com.abw12.absolutefitness.ordermgmtms.dto.request.VariantInventoryDTO;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductVariantDTO {
    @Id
    private String variantId;
    private String productId;
    private String variantName;
    private String variantValue;
    private String variantType;
    private String imagePath;
    private BigDecimal buyPrice;
    private BigDecimal onSalePrice;
    private String about;
    private String benefits;
    private String nutritionFacts;
    private String usageDose;
    private String manufacturerDetails;
    private Integer numberOfServings;
    private VariantInventoryDTO inventoryData;
    private String expiryDate;
    private String mfdDate;
    private String variantCreatedAt;
    private String variantModifiedAt;
}

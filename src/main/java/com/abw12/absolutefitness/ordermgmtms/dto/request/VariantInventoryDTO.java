package com.abw12.absolutefitness.ordermgmtms.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VariantInventoryDTO {

    private String variantInventoryId;
    private String variantId;
    private Long quantity;
    private String sku;
    private String createdAt;
    private String modifiedAt;
}

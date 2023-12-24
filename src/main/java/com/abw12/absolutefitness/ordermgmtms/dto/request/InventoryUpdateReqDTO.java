package com.abw12.absolutefitness.ordermgmtms.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryUpdateReqDTO {

    private String productInventoryId;
    private String variantId;
    private Long quantity;
    private String sku;
    private String createdAt;
    private String modifiedAt;
}

package com.abw12.absolutefitness.ordermgmtms.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryValidationReq {

    private String variantId;
    private Long quantityRequested;
}
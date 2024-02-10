package com.abw12.absolutefitness.ordermgmtms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderResDTO {

    private Integer amount;
    private String currency;
    private String orderId;
    private String pgOrderId;
    private String status;
    private String errMsg;
}

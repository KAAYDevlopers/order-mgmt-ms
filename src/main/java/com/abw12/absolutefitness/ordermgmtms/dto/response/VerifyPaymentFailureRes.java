package com.abw12.absolutefitness.ordermgmtms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VerifyPaymentFailureRes {

    private String status;
    private String message;
}

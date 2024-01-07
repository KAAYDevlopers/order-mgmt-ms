package com.abw12.absolutefitness.ordermgmtms.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VerifyPaymentReqDTO {

    @NotBlank
    private String razorPayOrderId;
    @NotBlank
    private String razorPayPaymentId;
    @NotBlank
    private String razorPayPaymentSignature;

}

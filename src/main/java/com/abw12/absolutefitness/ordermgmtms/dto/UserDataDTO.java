package com.abw12.absolutefitness.ordermgmtms.dto;

import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDataDTO {

    @Id
    private String userId;
    private String userName;
    @NotBlank
    private Long phoneNumber;
    @NotBlank
    private String emailId;
    private String billingAddress;
    @NotBlank
    private String shippingAddress;
}

package com.abw12.absolutefitness.ordermgmtms.dto.request;

import com.abw12.absolutefitness.ordermgmtms.dto.OrderItemDTO;
import com.abw12.absolutefitness.ordermgmtms.dto.UserDataDTO;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderReqDTO {

    private UserDataDTO userData;
    @NotEmpty
    private List<OrderItemDTO> orderItemList;
    @NotNull
    private BigDecimal totalAmount;
}

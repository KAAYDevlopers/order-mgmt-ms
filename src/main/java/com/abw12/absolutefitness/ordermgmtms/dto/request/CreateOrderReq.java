package com.abw12.absolutefitness.ordermgmtms.dto.request;

import com.abw12.absolutefitness.ordermgmtms.dto.OrderItemDTO;
import com.abw12.absolutefitness.ordermgmtms.dto.UserDataDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderReq {

    private UserDataDTO userData;
    private List<OrderItemDTO> orderItemList;
    private BigDecimal totalAmount;
}

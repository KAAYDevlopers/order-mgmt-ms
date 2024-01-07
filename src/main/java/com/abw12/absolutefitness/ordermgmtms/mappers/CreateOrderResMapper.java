package com.abw12.absolutefitness.ordermgmtms.mappers;

import com.abw12.absolutefitness.ordermgmtms.constants.CommonConstants;
import com.abw12.absolutefitness.ordermgmtms.dto.response.CreateOrderResDTO;
import com.razorpay.Order;
import org.springframework.stereotype.Component;

@Component
public class CreateOrderResMapper {

    public static CreateOrderResDTO mapToResponse(Order order){
        if(order == null) return null;

        CreateOrderResDTO createOrderResDTO = new CreateOrderResDTO();
        if(order.has(CommonConstants.ID))
            createOrderResDTO.setPgOrderId(order.get(CommonConstants.ID));
        if(order.has(CommonConstants.CURRENCY_KEY))
            createOrderResDTO.setCurrency(order.get(CommonConstants.CURRENCY_KEY));
        if(order.has(CommonConstants.AMOUNT))
            createOrderResDTO.setAmount(order.get(CommonConstants.AMOUNT));
        if(order.has(CommonConstants.STATUS_KEY))
            createOrderResDTO.setStatus(order.get(CommonConstants.STATUS_KEY));
        return createOrderResDTO;
    }
}

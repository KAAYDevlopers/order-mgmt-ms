package com.abw12.absolutefitness.ordermgmtms.mappers;

import com.abw12.absolutefitness.ordermgmtms.constants.CommonConstants;
import com.razorpay.Order;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CreateOrderResMapper {

    public static void mapToResponse(Order order,String orderId,Map<String,Object> responseMap){
        if(order == null) throw new RuntimeException("Order received cannot be Null while mapping to response...");

        if(!StringUtils.isEmpty(orderId))
            responseMap.put("orderId",orderId);
        if(order.has(CommonConstants.ID))
            responseMap.put("pgOrderId",order.get(CommonConstants.ID));
        if(order.has(CommonConstants.CURRENCY_KEY))
            responseMap.put("currency",order.get(CommonConstants.CURRENCY_KEY));
        if(order.has(CommonConstants.AMOUNT))
            responseMap.put("amount",order.get(CommonConstants.AMOUNT));
        if(order.has(CommonConstants.STATUS_KEY))
            responseMap.put("status",order.get(CommonConstants.STATUS_KEY));
    }
}

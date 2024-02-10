package com.abw12.absolutefitness.ordermgmtms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderStatusUpdateEvent {

    private String orderId;
    private Integer orderNumber;
    private String paymentId;
    private String userId;
//    private String orderStatus; //have to check if its needed
    private String paymentStatus;
    private String errorMsg;
}

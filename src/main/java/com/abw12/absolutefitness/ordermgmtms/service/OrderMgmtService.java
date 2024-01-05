package com.abw12.absolutefitness.ordermgmtms.service;

import com.abw12.absolutefitness.ordermgmtms.constants.CommonConstants;
import com.abw12.absolutefitness.ordermgmtms.dto.UserDataDTO;
import com.abw12.absolutefitness.ordermgmtms.dto.request.CreateOrderReq;
import com.abw12.absolutefitness.ordermgmtms.entity.OrderEntity;
import com.abw12.absolutefitness.ordermgmtms.gateway.implementation.CreateOrderRestImpl;
import com.abw12.absolutefitness.ordermgmtms.repository.OrderManagementRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorpay.Order;
import com.razorpay.RazorpayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class OrderMgmtService {

    @Autowired
    private CreateOrderRestImpl createOrderRest;
    @Autowired
    private OrderManagementRepository orderManagementRepository;
    @Autowired
    private ObjectMapper objectMapper;

    private static final Logger logger = LoggerFactory.getLogger(OrderMgmtService.class);

    public Map<String, String> createOrder(CreateOrderReq request) throws RazorpayException {
        logger.info("Processing the order request...");
        OrderEntity orderData = generateOrder(request);
        logger.info("Initiating the Payment Gateway create order API");
        Order pgOrderRes = createOrderRest.initiateCreateOrderPGReq(request);
        if(pgOrderRes.has(CommonConstants.ID))
            orderData.setPgOrderId(pgOrderRes.get(CommonConstants.ID));
        else throw new RuntimeException(String.format("payment gateway Response does not contain order id :: %s",pgOrderRes));
        //store order data in db
        storeOrderDataInDB(orderData);
        Map<String,String> response = new HashMap<>();
        response.put("order_id",pgOrderRes.get(CommonConstants.ID));
        return response; //return payment gateway order response to UI
    }

    private void storeOrderDataInDB(OrderEntity orderData) {
        logger.info("Storing Order data with payment gateway orderId={}",orderData.getPgOrderId());
        OrderEntity storedOrderInDB = orderManagementRepository.save(orderData);
        logger.info("Successfully created an Order in db with Internal orderId={}  => {}",storedOrderInDB.getOrderId(),storedOrderInDB);
    }


    private OrderEntity generateOrder(CreateOrderReq req) {
        OrderEntity orderData = new OrderEntity();
        UserDataDTO userData = req.getUserData();
        if(userData!=null ){
            if(!userData.getUserId().isEmpty())
                orderData.setUserId(userData.getUserId());
            if(!userData.getBillingAddress().isEmpty())
                orderData.setBillingAddress(userData.getBillingAddress());
            if(!userData.getShippingAddress().isEmpty())
                orderData.setShippingAddress(userData.getShippingAddress());
        }else {
            throw new RuntimeException(String.format("User Data Cannot be Null/Empty ... InValid Order Request %s",req));
        }
        if(req.getTotalAmount()!=null)
            orderData.setTotalAmount(req.getTotalAmount());
        orderData.setCurrency(CommonConstants.CURRENCY_VALUE_INR);
        orderData.setOrderStatus(CommonConstants.ORDER_STATUS_PENDING); //while initiating the order status will always be PENDING
        orderData.setOrderCreatedAt(OffsetDateTime.now());
//        paymentId will set based on payment gateway response
        return orderData;
    }


}

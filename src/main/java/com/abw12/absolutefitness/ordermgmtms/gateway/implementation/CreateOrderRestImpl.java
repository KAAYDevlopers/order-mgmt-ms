package com.abw12.absolutefitness.ordermgmtms.gateway.implementation;

import com.abw12.absolutefitness.ordermgmtms.constants.CommonConstants;
import com.abw12.absolutefitness.ordermgmtms.dto.request.CreateOrderReq;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Random;

@Component
public class CreateOrderRestImpl {

    @Value("${payment.razorpay.api.keyId}")
    private String keyId;
    @Value("${payment.razorpay.api.secret}")
    private String secret;
    @Value("{payment.razorpay.api.createOrderURL}")
    private String createOrderUrl;
    @Autowired
    private ObjectMapper objectMapper;

    private static final Logger logger = LoggerFactory.getLogger(CreateOrderRestImpl.class);


    public Order initiateCreateOrderPGReq(CreateOrderReq orderReq){
        Random rand = new Random();
        JSONObject req = new JSONObject();
        req.put(CommonConstants.AMOUNT,orderReq.getTotalAmount().intValue() * 100);
        req.put(CommonConstants.CURRENCY_KEY,CommonConstants.CURRENCY_VALUE_INR);
        req.put(CommonConstants.RECEIPT, String.valueOf(rand.nextInt(10000)));
        JSONObject notes = new JSONObject();
        notes.put("notes_key_1","Test-1");
        notes.put("notes_key_2","Test-2");
        req.put(CommonConstants.NOTES,notes);
        Order orderResponse;
        try {
            RazorpayClient client = new RazorpayClient(keyId,secret);
            orderResponse = client.orders.create(req);
            logger.info("Create Order Response from Payment Gateway :: {}",orderResponse);
        } catch (RazorpayException e) {
            logger.error("Error while calling create order payment gateway API :: {}",e.getMessage());
            throw new RuntimeException(e);
        }
        return orderResponse;
    }
}

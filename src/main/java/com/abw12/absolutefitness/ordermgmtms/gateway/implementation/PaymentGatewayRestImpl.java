package com.abw12.absolutefitness.ordermgmtms.gateway.implementation;

import com.abw12.absolutefitness.ordermgmtms.constants.CommonConstants;
import com.abw12.absolutefitness.ordermgmtms.advice.ErrorResponse;
import com.abw12.absolutefitness.ordermgmtms.dto.request.CreateOrderReqDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorpay.Order;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class PaymentGatewayRestImpl {

    @Value("${payment.razorpay.api.keyId}")
    private String keyId;
    @Value("${payment.razorpay.api.secret}")
    private String secret;
    @Value("{payment.razorpay.api.createOrderURL}")
    private String createOrderUrl;
    @Autowired
    private ObjectMapper objectMapper;

    private static final Logger logger = LoggerFactory.getLogger(PaymentGatewayRestImpl.class);


    public Order initiateCreateOrderPGReq(CreateOrderReqDTO orderReq){
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

    public Payment fetchPaymentDetails(String pgPaymentId){
        Payment paymentResponse;
        try {
            RazorpayClient client = new RazorpayClient(keyId,secret);
            paymentResponse = client.payments.fetch(pgPaymentId);
            if(paymentResponse.has("error")){
                ErrorResponse errorResponse = objectMapper.convertValue(paymentResponse, ErrorResponse.class);
                logger.error("Failed to fetch payment details,failure cause :: {}",errorResponse.getError().getDescription());
                throw new RuntimeException(String.format("Error while fetching payment details from razorpay for razorpay_payment_id=%s :: cause =>",pgPaymentId,errorResponse));
            }
        } catch (RazorpayException e) {
            logger.error("Could not fetch payment details from razorpay server for razorpay_payment_id = {} :: errorMessage = {}",pgPaymentId,e.getMessage());
            throw new RuntimeException(e);
        }
        return paymentResponse;
    }
}

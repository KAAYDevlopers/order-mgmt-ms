package com.abw12.absolutefitness.ordermgmtms.controller;

import com.abw12.absolutefitness.ordermgmtms.dto.request.CreateOrderReqDTO;
import com.abw12.absolutefitness.ordermgmtms.dto.request.VerifyPaymentReqDTO;
import com.abw12.absolutefitness.ordermgmtms.service.OrderMgmtService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order-mgmt-ms")
public class OrderMgmtController {

    private static final Logger logger = LoggerFactory.getLogger(OrderMgmtController.class);
    @Autowired
    private OrderMgmtService orderMgmtService;
    @PostMapping("/createTransactionAndPlaceOrder")
    private ResponseEntity<?> createTransactionAndPlaceOrder(@RequestBody @Valid CreateOrderReqDTO request){
        logger.info("Inside place order controller :: order request received.");
        try{
            return new ResponseEntity<>(orderMgmtService.createTransactionAndPlaceOrder(request), HttpStatus.OK);
        }catch (Exception ex){
            logger.error("Exception while placing the order: {}", ex.getMessage());
            return new ResponseEntity<>("Failed to place the order,try again later",HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/verify-payment/{orderId}")
    private ResponseEntity<?> verifyPayment(@RequestBody @Valid VerifyPaymentReqDTO request, @PathVariable String orderId){
        logger.info("Inside verify-payment controller :: payment details received for internal orderId={}",orderId);
        try{
            return new ResponseEntity<>(orderMgmtService.verifyPaymentWithOrderId(request,orderId), HttpStatus.OK);
        }catch (Exception ex){
            logger.error("Exception while verifying the payment : {}", ex.getMessage());
            return new ResponseEntity<>("Failed to verify the payment,encountered exception!",HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}

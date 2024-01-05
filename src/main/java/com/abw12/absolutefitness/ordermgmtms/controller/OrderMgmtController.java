package com.abw12.absolutefitness.ordermgmtms.controller;

import com.abw12.absolutefitness.ordermgmtms.dto.request.CreateOrderReq;
import com.abw12.absolutefitness.ordermgmtms.service.OrderMgmtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order-mgmt-ms")
public class OrderMgmtController {

    private static final Logger logger = LoggerFactory.getLogger(OrderMgmtController.class);
    @Autowired
    private OrderMgmtService orderMgmtService;
    @PostMapping("/createOrder")
    private ResponseEntity<?> placeOrder(@RequestBody CreateOrderReq request){
        logger.info("Inside place order controller :: order request received.");
        try{
            return new ResponseEntity<>(orderMgmtService.createOrder(request), HttpStatus.OK);
        }catch (Exception ex){
            logger.error("Exception while placing the order: {}", ex.getMessage());
            return new ResponseEntity<>("Failed to place the order,try again later",HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}

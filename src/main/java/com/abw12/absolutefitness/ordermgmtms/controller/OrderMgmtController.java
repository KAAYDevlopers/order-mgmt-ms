package com.abw12.absolutefitness.ordermgmtms.controller;

import com.abw12.absolutefitness.ordermgmtms.dto.OrderStatusUpdateEvent;
import com.abw12.absolutefitness.ordermgmtms.dto.request.CreateOrderReqDTO;
import com.abw12.absolutefitness.ordermgmtms.service.OrderMgmtService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

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

//    @PostMapping("/verify-payment/{orderId}")
//    private ResponseEntity<?> verifyPayment(@RequestBody @Valid VerifyPaymentReqDTO request, @PathVariable String orderId){
//        logger.info("Inside verify-payment controller :: payment details received for internal orderId={}",orderId);
//        try{
//            VerifyPaymentResponse verifyPaymentResponse = orderMgmtService.verifyPaymentWithOrderId(request, orderId);
//            if(verifyPaymentResponse.getStatus().equals(CommonConstants.SUCCESS)){
//                return new ResponseEntity<>(verifyPaymentResponse,HttpStatus.OK);
//            }else{
//                logger.error("Payment Verification Failed :: {}",verifyPaymentResponse);
//                return new ResponseEntity<>(verifyPaymentResponse, HttpStatus.INTERNAL_SERVER_ERROR);
//            }
//        }catch (Exception ex){
//            logger.error("Exception while verifying the payment : {}", ex.getMessage());
//            return new ResponseEntity<>("Failed to verify the payment,encountered exception!",HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }

    @GetMapping("/get-order/{orderId}")
    private ResponseEntity<?> getOrderById(@RequestParam String orderId){
        logger.info("Inside get order controller.");
        try{
            return new ResponseEntity<>(orderMgmtService.getOrderDetails(orderId), HttpStatus.OK);
        }catch (Exception ex){
            logger.error("Exception while fetching the order details with orderId: {} => {}", orderId,ex.getMessage());
            return new ResponseEntity<>("Failed to fetch the order,try again later",HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/fetchOrderHistory/{userId}")
    private ResponseEntity<?> fetchOrderHistory(@PathVariable String userId){
        logger.info("Inside fetchOrderHistory API call...");
        try{
            return new ResponseEntity<>(orderMgmtService.fetchOrderHistory(userId), HttpStatus.OK);
        }catch (Exception ex){
            logger.error("Exception while fetching the order history for userId: {} => {}", userId,ex.getMessage());
            return new ResponseEntity<>("Failed to fetch the order history,try again later",HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/api/webhook/razorpay")
    private ResponseEntity<?> handleRazorPayWebhook(@RequestBody String event , @RequestParam  ("X-Razorpay-Signature") String receivedSignature){
        logger.info("Received Razorpay payment event :: {}",event);
        try{
            orderMgmtService.handleWebhookEvent(event, receivedSignature);
            return ResponseEntity.ok("Webhook processed successfully");
        }catch (Exception ex){
            logger.error("Exception while processing the webhook for event =>  Error :: {}",ex.getMessage(),ex.getCause());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing webhook");
        }
    }

    @GetMapping(path= "/order-updates/{userId}",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<OrderStatusUpdateEvent>> getOrderUpdates(@PathVariable String userId ){
        logger.info("SSE Controller Invoked by client for Order Update with UserId={}",userId);
        try{
            return orderMgmtService.orderStatusEventStream(userId);
        }catch (Exception e){
            logger.error("Error while streaming the order-update events :: {}",e.getMessage(),e.getCause());
            return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

}

package com.abw12.absolutefitness.ordermgmtms.service;

import com.abw12.absolutefitness.ordermgmtms.constants.CommonConstants;
import com.abw12.absolutefitness.ordermgmtms.constants.PaymentEventType;
import com.abw12.absolutefitness.ordermgmtms.dto.*;
import com.abw12.absolutefitness.ordermgmtms.dto.request.CreateOrderReqDTO;
import com.abw12.absolutefitness.ordermgmtms.entity.OrderEntity;
import com.abw12.absolutefitness.ordermgmtms.entity.OrderItemEntity;
import com.abw12.absolutefitness.ordermgmtms.entity.PaymentEntity;
import com.abw12.absolutefitness.ordermgmtms.gateway.implementation.PaymentGatewayRestImpl;
import com.abw12.absolutefitness.ordermgmtms.helper.HelperUtils;
import com.abw12.absolutefitness.ordermgmtms.helper.JsonParser;
import com.abw12.absolutefitness.ordermgmtms.mappers.CreateOrderResMapper;
import com.abw12.absolutefitness.ordermgmtms.mappers.OrderItemsMapper;
import com.abw12.absolutefitness.ordermgmtms.mappers.OrderMapper;
import com.abw12.absolutefitness.ordermgmtms.repository.OrderItemRepository;
import com.abw12.absolutefitness.ordermgmtms.repository.OrderManagementRepository;
import com.abw12.absolutefitness.ordermgmtms.repository.PaymentEntityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorpay.Order;
import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class OrderMgmtService {

    @Autowired
    private PaymentGatewayRestImpl paymentGatewayRest;
    @Autowired
    private OrderManagementRepository orderManagementRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private PaymentEntityRepository paymentEntityRepository;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemsMapper orderItemsMapper;
    @Autowired
    private HelperUtils helperUtils;
    @Autowired
    private ObjectMapper objectMapper;


    private static final Logger logger = LoggerFactory.getLogger(OrderMgmtService.class);

    //Help building Sinks.Many that will broadcast signals to multiple Subscriber
    private final Sinks.Many<OrderStatusUpdateEvent> sink = Sinks.many().multicast().onBackpressureBuffer();


    /**
     * @param request UI collects the userdata, product variant data from cart and calculated total amount and send to
     *                createTransactionAndPlaceOrder API.
     * @return   object containing mainly the orderId(internal use),pgOrderId received from initiate payment gateway API and few other required fields.
     */
    @Transactional
    public Map<String,Object> createTransactionAndPlaceOrder(CreateOrderReqDTO request) {
        logger.info("Processing the order request...");
        Map<String,Object> responseMap=new HashMap<>();
        //check the stock status for all the orderItems in request if any orderItem in the entire order is out of stock cancel the request and response failure to client
        if(!helperUtils.checkStockStatus(request)){
            responseMap.put("errMsg","Insufficient stock for one of the OrderItem in the requested Order");
            return responseMap;
        }
        OrderEntity orderData = helperUtils.generateOrder(request);
        logger.info("Initiating the Payment Gateway create order API");
        //call initiate order(transaction in razorpay) API Call and create order in payment gateway
        Order pgOrderRes = paymentGatewayRest.initiateCreateOrderPGReq(request);
        if(pgOrderRes.has(CommonConstants.ID))
            orderData.setPgOrderId(pgOrderRes.get(CommonConstants.ID));
        else throw new RuntimeException(String.format("payment gateway Response does not contain order id :: %s",pgOrderRes));
        //store order data in db
        OrderEntity storedOrderInDB = storeOrderDataInDB(orderData);

        //store all the orderItem inside the request related to the particular orderId in db
        List<OrderItemEntity> orderItemEntities = helperUtils.generateOrderItemsList(request.getOrderItemList(), storedOrderInDB.getOrderId());
        List<OrderItemDTO> storedOrderItems = orderItemEntities.stream().map(orderItemEntity -> orderItemsMapper.entityToDto(orderItemRepository.save(orderItemEntity))).toList();
        logger.info("Order item stored in DB for orderId={} => {}",storedOrderInDB.getOrderId(),storedOrderItems);
        // TODO: 20-01-2024 Need to decide how reserved quantity will work in case of simultaneous request for same variant
        //  currently just set the reserved quantity and set the flag true
        //reserve the variant for requested quantity in product variant inventory
        helperUtils.reserveVariantInventory(storedOrderItems);
        logger.info("reserve variant inventory quantity update API call completed.");
        //convert order obj from payment gateway into responseMap and set internal orderId for ref
        CreateOrderResMapper.mapToResponse(pgOrderRes,storedOrderInDB.getOrderId(),responseMap);
//       responseMap.setOrderId(storedOrderInDB.getOrderId());
        logger.info("Returning create order responseMap to client :: {} ",responseMap);
        return responseMap; //return payment gateway order responseMap to UI
    }



    @Transactional(readOnly = true)
    public OrderDTO getOrderDetails(String orderId){
        logger.info("Fetching order details by orderId={}",orderId);
        OrderEntity orderData = orderManagementRepository.getOrderById(orderId).orElseThrow(() ->
                new RuntimeException(String.format("Cannot find order details by orderId : %s", orderId)));
        OrderDTO response = orderMapper.entityToDto(orderData);
        List<OrderItemEntity> orderItemEntities = orderItemRepository.fetchOrderItemsByOderId(orderId).orElseThrow(() ->
                new RuntimeException(String.format("Error While fetching orderItem list for orderId : %s", orderId)));
        List<OrderItemDTO> orderItemList = orderItemEntities.stream().map(orderItemEntity -> orderItemsMapper.entityToDto(orderItemEntity)).toList();
        response.setOrderItems(orderItemList);
        logger.info("Fetched order details with orderId={} => {}",orderId,response);
        return response;
    }

    @Transactional(readOnly = true)
    public List<OrderHistoryDTO> fetchOrderHistory(String userId){
        logger.info("Fetching Order History for userId={}",userId);
        List<OrderEntity> ordersList = orderManagementRepository.getOrderByUserId(userId).orElseThrow(() ->
                new RuntimeException(String.format("Cannot find order details by userId : %s", userId)));
        logger.info("Order details fetched for userId={} => Found {} order",userId,ordersList.size());
        List<OrderHistoryDTO> orderHistoryRes = ordersList.stream().map(orderEntity -> {
            String orderId = orderEntity.getOrderId();
            List<OrderItemEntity> orderItemList = orderItemRepository.fetchOrderItemsByOderId(orderId).orElseThrow(() ->
                    new RuntimeException(String.format("Cannot find orderItems by orderId : %s", orderId)));
            List<OrderItemHistory> orderItemHistoryList = orderItemList.stream().map(orderItem -> {
                String variantId = orderItem.getVariantId();
                // API call to product-catalog-ms to fetch variant info
                ProductVariantDTO productVariantData = helperUtils.fetchVariantData(variantId);
                return helperUtils.constructOrderHistoryItem(productVariantData, orderItem);
            }).toList();
            return new OrderHistoryDTO(orderEntity.getOrderNumber(), orderEntity.getTotalAmount(), orderEntity.getShippingAddress(),
                    orderEntity.getBillingAddress(), orderItemHistoryList, helperUtils.offsetDateTimeFormatter(orderEntity.getOrderPlacedDate()));
        }).toList();
        logger.info("Successfully fetched order history for userID={} => {}",userId,orderHistoryRes);
        return orderHistoryRes;
    }

    /**
     * @param eventDataReceived Event data received from razorpay webhook api call.
     * @param receivedSignature payment signature received in handler to validate the authenticity of the payment information whether its from razorpay or not.
     */
    @Transactional
    public void handleWebhookEvent(String eventDataReceived, String receivedSignature){
        logger.info("Verifying the Event received for razorpay webhook");
        // Validate the signature
//        boolean isPaymentSignatureVerified = helperUtils.verifyWebhookSignature(eventDataReceived, receivedSignature);
//        logger.info("Event signature is verified={}",isPaymentSignatureVerified);
        WebhookEventRes webhookEventRes;
        try {
            webhookEventRes= JsonParser.parseJson(eventDataReceived, WebhookEventRes.class);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to parse the eventDataReceived :: Error Message => %s",e.getMessage()));
        }
        //retrieve razorpay order_id from eventDataReceived
        String pgOrderId=null;
        Map<String, Object> paymentMap;
        Map<String, Object> entityMap = null;
        if (eventDataReceived != null && webhookEventRes.getPayload() != null) {
            paymentMap = (Map<String, Object>) webhookEventRes.getPayload().get("payment");
            if (paymentMap != null) {
                entityMap = (Map<String, Object>) paymentMap.get("entity");
                if (entityMap != null) {
                    pgOrderId = (String) entityMap.get("order_id");
                }
            }
        }

        String eventType = webhookEventRes.getEvent();
        logger.info("Event Type {} received",eventType);

        //Fetch the order details from DB
        String finalPgOrderId = pgOrderId; //copy to temp variable to use in lambda
        logger.info("Fetch order from DB with pgOrderId={}",finalPgOrderId);
        OrderEntity existingOrderInDB = orderManagementRepository.getOrderByPgOrderId(finalPgOrderId).orElseThrow(() ->
                new RuntimeException(String.format("Cannot find order details by pgOrderId : %s", finalPgOrderId)));
        logger.info("Order retrieved with pgOrderId={} :: Data={}",finalPgOrderId,existingOrderInDB);

        String orderId = existingOrderInDB.getOrderId(); //internal orderId
//        if(!isPaymentSignatureVerified){
//            logger.error("Payment Signature verification failed for received event");
//            //set status to verification failed
//            existingOrderInDB.setOrderStatus(CommonConstants.ORDER_STATUS_PAYMENT_VERIFICATION_FAILED);
//            //store payment details in DB
//            PaymentEntity paymentEntityStored = storePaymentDetailsInDBWebhook(orderId, entityMap);
//            existingOrderInDB.setPaymentId(paymentEntityStored.getPaymentId()); //internal paymentId
//            existingOrderInDB.setPaymentSignatureVerification(false);
//            existingOrderInDB.setOrderModifiedAt(OffsetDateTime.parse(OffsetDateTime.now().format(HelperUtils.dateFormat())));
//            //update the order details in db for failed signature verification to keep track of such orders for further analysis
//            OrderEntity storedOrderData = orderManagementRepository.save(existingOrderInDB);
//            // TODO: 20-01-2024 Need to decide what response should be given to client and if payment signature failed do we have to refund the amount or not
//            throw new RuntimeException(String.format("Invalid Signature for webhook eventDataReceived :: Order and payment Details stored in DB for further analysis, orderId=%s",
//                    storedOrderData.getOrderId()));
//        }
        //payment signature verification done then handle the different eventType
        switch (eventType){
            case PaymentEventType.AUTHORIZED -> {
                if(existingOrderInDB.getOrderStatus().equalsIgnoreCase(CommonConstants.ORDER_STATUS_PENDING)){
                    //normal paymentAuthorization logic will be processed
                    handlePaymentAuthorized(existingOrderInDB,entityMap,orderId,eventType);
                }
                else if (existingOrderInDB.getOrderStatus().equalsIgnoreCase(CommonConstants.ORDER_STATUS_CAPTURED_PENDING_AUTHORIZATION))
                {
                    logger.info("Order with orderId={} is already in captured state with status={}, marking order as authorized and proceeding with capturing process",
                            orderId,existingOrderInDB.getOrderStatus());
                    markPaymentAsCaptured(existingOrderInDB,entityMap,orderId,eventType);
                }else if (existingOrderInDB.getOrderStatus().equalsIgnoreCase(CommonConstants.ORDER_STATUS_PAYMENT_AUTHORIZED)) {
                    logger.info("Duplicate event received for oderId={} existing state in DB for Order is already in authorized state Order Details={}",
                            orderId,existingOrderInDB);
                }
            }
            // TODO: 20-01-2024 Need to decide how Reconciliation will work to process order in CAPTURED_PENDING_AUTHORIZATION state for long time if authorized event is not received for them
            case PaymentEventType.CAPTURED -> {
                //captured event is received first before authorization event (out of order event scenario)
                if(existingOrderInDB.getOrderStatus().equalsIgnoreCase(CommonConstants.ORDER_STATUS_PENDING)){
                    //only mark the order with authorization pending
                    markPaymentAsAuthorizationPending(existingOrderInDB,entityMap,orderId,eventType);
                }else if(existingOrderInDB.getOrderStatus().equalsIgnoreCase(CommonConstants.ORDER_STATUS_PAYMENT_AUTHORIZED)){
                    //Normal captured event logic is processed
                    markPaymentAsCaptured(existingOrderInDB,entityMap,orderId,eventType);
                } else if (existingOrderInDB.getOrderStatus().equalsIgnoreCase(CommonConstants.ORDER_STATUS_PAYMENT_CAPTURED)) {
                    logger.info("Duplicate event received for oderId={} existing state in DB for Order is already in captured state Order Details={}"
                            ,orderId,existingOrderInDB);
                }
            }
            case PaymentEventType.FAILED -> {
                handlePaymentFailed(existingOrderInDB,entityMap,orderId,eventType);
            }
            default -> throw new IllegalStateException(String.format("Unknown event received with eventType = %s for orderId = %s",eventType,orderId));
        }
    }

    private void handlePaymentAuthorized(OrderEntity existingOrderInDB,Map<String,Object> entityMap,String orderId,String eventType) {
        logger.info("Event Type {} is Processing...",eventType);
        existingOrderInDB.setOrderStatus(CommonConstants.ORDER_STATUS_PAYMENT_AUTHORIZED);
        //store payment details in DB
        PaymentEntity paymentEntityStored = storePaymentDetailsInDBWebhook(orderId, entityMap);
        existingOrderInDB.setPaymentId(paymentEntityStored.getPaymentId());
        existingOrderInDB.setPaymentSignatureVerification(Boolean.TRUE);
        existingOrderInDB.setOrderModifiedAt(OffsetDateTime.parse(OffsetDateTime.now().format(HelperUtils.dateFormat())));
        //update the order details in db
        OrderEntity storedOrderData = orderManagementRepository.save(existingOrderInDB);
        logger.info("Successfully stored the Order & Payment data in DB for {} event received with orderId={}",eventType,orderId);
        //update product inventory data
//        List<OrderItemEntity> orderItems = orderItemRepository.fetchOrderItemsByOderId(orderId).orElseThrow(() ->
//                new RuntimeException(String.format("Error While fetching orderItem list for orderId : %s", orderId)));
//        helperUtils.updateVariantInventoryWebhook(orderItems,eventType);
//        logger.info("Done updating Product Variant Inventory data for orderId={}",orderId);
        //create response event for the UI and publish to the event stream
        OrderStatusUpdateEvent orderStatusUpdateEvent = helperUtils.constructOrderUpdateEvent(orderId, storedOrderData, paymentEntityStored);
        sendOrderUpdate(orderStatusUpdateEvent);
    }

    /**
     * @param existingOrderInDB
     * @param entityMap
     * @param orderId
     * @param eventType
     *  This method will mark the order as pending for authorization in DB if captured event received before authorized event.
     */
    private void markPaymentAsAuthorizationPending(OrderEntity existingOrderInDB, Map<String,Object> entityMap, String orderId, String eventType) {
        logger.info("Event Type {} is Processing :: Captured event received before authorized event for orderId={}",eventType,orderId);
        existingOrderInDB.setOrderStatus(CommonConstants.ORDER_STATUS_CAPTURED_PENDING_AUTHORIZATION);
        logger.info("marked Order with orderId={} as CAPTURED_PENDING_AUTHORIZATION.",orderId);
        //store payment details in DB
        PaymentEntity paymentEntityStored = storePaymentDetailsInDBWebhook(orderId, entityMap);
        existingOrderInDB.setPaymentId(paymentEntityStored.getPaymentId());
        existingOrderInDB.setPaymentSignatureVerification(Boolean.TRUE);
        existingOrderInDB.setOrderModifiedAt(OffsetDateTime.parse(OffsetDateTime.now().format(HelperUtils.dateFormat())));
        //update the order details in db
        OrderEntity storedOrderData = orderManagementRepository.save(existingOrderInDB);
        logger.info("Successfully stored the Order & Payment data in DB for {} event received with orderId={}",eventType,orderId);
        //update product inventory data
        List<OrderItemEntity> orderItems = orderItemRepository.fetchOrderItemsByOderId(orderId).orElseThrow(() ->
                new RuntimeException(String.format("Error While fetching orderItem list for orderId : %s", orderId)));
        helperUtils.updateVariantInventoryWebhook(orderItems,eventType);
        logger.info("Done updating Product Variant Inventory data for orderId={}",orderId);
        //create response event for the UI and publish to the event stream
        OrderStatusUpdateEvent orderStatusUpdateEvent = helperUtils.constructOrderUpdateEvent(orderId, storedOrderData, paymentEntityStored);
        sendOrderUpdate(orderStatusUpdateEvent);
    }

    private void markPaymentAsCaptured(OrderEntity existingOrderInDB, Map<String,Object> entityMap, String orderId, String eventType) {
        logger.info("Event Type {} is Processing...",eventType);
        existingOrderInDB.setOrderStatus(CommonConstants.ORDER_STATUS_COMPLETED);
        //store payment details in DB
        PaymentEntity paymentEntityStored = storePaymentDetailsInDBWebhook(orderId, entityMap);
        existingOrderInDB.setPaymentId(paymentEntityStored.getPaymentId());
        existingOrderInDB.setPaymentSignatureVerification(Boolean.TRUE);
        existingOrderInDB.setOrderModifiedAt(OffsetDateTime.parse(OffsetDateTime.now().format(HelperUtils.dateFormat())));
        existingOrderInDB.setOrderPlacedDate(OffsetDateTime.parse(OffsetDateTime.now().format(HelperUtils.dateFormat())));
        existingOrderInDB.setOrderNumber( Math.abs(new Random().nextInt()));
        //update the order details in db
        OrderEntity storedOrderData = orderManagementRepository.save(existingOrderInDB);
        logger.info("Successfully stored the Order & Payment data in DB for {} event received with orderId={}",eventType,orderId);
        //update product inventory data
        List<OrderItemEntity> orderItems = orderItemRepository.fetchOrderItemsByOderId(orderId).orElseThrow(() ->
                new RuntimeException(String.format("Error While fetching orderItem list for orderId : %s", orderId)));
        helperUtils.updateVariantInventoryWebhook(orderItems,eventType);
        logger.info("Done updating Product Variant Inventory data for orderId={}",orderId);
        // TODO: 20-01-2024 Need to work on trigger email/sms notification of order confirmation
        //create response event for the UI and publish to the event stream
        OrderStatusUpdateEvent orderStatusUpdateEvent = helperUtils.constructOrderUpdateEvent(orderId, storedOrderData, paymentEntityStored);
        sendOrderUpdate(orderStatusUpdateEvent);
    }

    private void handlePaymentFailed(OrderEntity existingOrderInDB,Map<String,Object> entityMap,String orderId,String eventType) {
        logger.info("Event Type {} is Processing...",eventType);
        existingOrderInDB.setOrderStatus(CommonConstants.ORDER_STATUS_FAILED);
        //store payment details in DB
        PaymentEntity paymentEntityStored = storePaymentDetailsInDBWebhook(orderId, entityMap);
        existingOrderInDB.setPaymentId(paymentEntityStored.getPaymentId());
        existingOrderInDB.setPaymentSignatureVerification(Boolean.TRUE);
        existingOrderInDB.setOrderModifiedAt(OffsetDateTime.parse(OffsetDateTime.now().format(HelperUtils.dateFormat())));
        //update the order details in db
        OrderEntity storedOrderData = orderManagementRepository.save(existingOrderInDB);
        logger.info("Stored the Order & Payment data in DB for {} event received with orderId={} for further analysis",eventType,orderId);
        //update product inventory data to release the stock of reserved variant
        logger.info("Calling update inventory API to release all the variants stock on hold for orderId={}",orderId);
        List<OrderItemEntity> orderItems = orderItemRepository.fetchOrderItemsByOderId(orderId).orElseThrow(() ->
                new RuntimeException(String.format("Error While fetching orderItem list for orderId : %s", orderId)));
        helperUtils.updateVariantInventoryWebhook(orderItems,eventType);
        //create response event for the UI and publish to the event stream
        OrderStatusUpdateEvent orderStatusUpdateEvent = helperUtils.constructOrderUpdateEvent(orderId, storedOrderData, paymentEntityStored);
        sendOrderUpdate(orderStatusUpdateEvent);
    }

    private OrderEntity storeOrderDataInDB(OrderEntity orderData) {
        logger.info("Storing Order data with payment gateway orderId={}",orderData.getPgOrderId());
        OrderEntity storedOrderInDB = orderManagementRepository.save(orderData);
        if(StringUtils.isEmpty(storedOrderInDB.getOrderId())) throw new RuntimeException("OrderId cannot be NULL/Empty for stored order data in DB");
        logger.info("Successfully created an Order in db with Internal orderId={}  => {}",storedOrderInDB.getOrderId(),storedOrderInDB);
        return storedOrderInDB;
    }

    private PaymentEntity storePaymentDetailsInDBWebhook(String orderId,Map<String,Object> razorpayPaymentEntity){
        //store payment details in db
        PaymentEntity paymentDataStored;
        Optional<PaymentEntity> existingPaymentData = paymentEntityRepository.getPaymentEntityByOrderId(orderId);
        if(existingPaymentData.isPresent()){
            if(razorpayPaymentEntity.containsKey(CommonConstants.STATUS_KEY)
                    && razorpayPaymentEntity.get(CommonConstants.STATUS_KEY) !=null ){
                existingPaymentData.get().setPaymentStatus(String.valueOf(razorpayPaymentEntity.get(CommonConstants.STATUS_KEY)));
                existingPaymentData.get().setPaymentModifiedDate(OffsetDateTime.now());
            }
            paymentDataStored= paymentEntityRepository.save(existingPaymentData.get());
        }else {
            logger.info("Creating new Payment Entity in DB for orderId={}",orderId);
            PaymentEntity paymentData = helperUtils.preparePaymentData( orderId, razorpayPaymentEntity);
            paymentDataStored= paymentEntityRepository.save(paymentData);
        }
        logger.info("Payment details stored successfully in db for orderId = {} => {}",orderId,paymentDataStored);
        return paymentDataStored;
    }

    /**
     * @param userId the user for which order update are requested
     * @return This Flux will emit items to its subscribers whenever a new value is pushed into the sink. response is given to client in ServerSentEvent format
     *
     */
    public Flux<ServerSentEvent<OrderStatusUpdateEvent>> orderStatusEventStream(String userId){
        return sink.asFlux()
                .filter(updateEvent -> updateEvent.getUserId().equals(userId))
                .map(update -> ServerSentEvent.builder(update).build())
                .doOnError(error -> logger.error("Error in SSE Stream OrderUpdate for UserId={} :: ERROR => {}",userId,error.getMessage()))
                .onErrorResume(error -> Flux.just(ServerSentEvent.builder(errorEventResponse(error)).build()))
                .mergeWith(heartbeat());
    }

    public void sendOrderUpdate(OrderStatusUpdateEvent updateEvent){
        //emitting the events in the sink from which the client will read it by subscribing to the stream
        sink.tryEmitNext(updateEvent);
    }

    public OrderStatusUpdateEvent errorEventResponse(Throwable err){
        OrderStatusUpdateEvent errorEvent = new OrderStatusUpdateEvent();
        errorEvent.setErrorMsg("Error Occurred :: " + err.getMessage());
        return errorEvent;
    }


    /** This method keeps the connection alive with client by sending a ping on event stream every 30seconds
     * @return creates a Flux that emits a long sequence of increasing numbers, starting from 0, at fixed 30-second intervals.
     */
    private Publisher<ServerSentEvent<OrderStatusUpdateEvent>> heartbeat() {
        return Flux.interval(Duration.ofSeconds(30))
                .map(seq -> ServerSentEvent.<OrderStatusUpdateEvent>builder()
                        .comment("Keep-alive") // comments are a way to send data that should be ignored by the client application.
                        .build());
    }


    //    private PaymentEntity storePaymentDetailsInDB(VerifyPaymentReqDTO req,String orderId){
//        //fetch payment details from the razorpay payment gateway
//        Payment paymentDetailsFetched = paymentGatewayRest.fetchPaymentDetails(req.getRazorPayPaymentId());
//
//        //store payment details in db
//        PaymentEntity paymentData = helperUtils.preparePaymentData(req.getRazorPayPaymentId(), orderId, paymentDetailsFetched);
//        PaymentEntity paymentDataStored = paymentEntityRepository.save(paymentData);
//        logger.info("Payment details stored successfully in db for orderId = {} => {}",orderId,paymentDataStored);
//        return paymentDataStored;
//    }

    //    @Transactional
//    public VerifyPaymentResponse verifyPaymentWithOrderId(VerifyPaymentReqDTO req , String orderId){
//        if(req == null ||
//                StringUtils.isEmpty(req.getRazorPayOrderId())
//                        && StringUtils.isEmpty(req.getRazorPayPaymentId())
//                        && StringUtils.isEmpty(req.getRazorPayPaymentSignature())
//        ) throw new RuntimeException("Invalid Request VerifyPaymentReq is NULL or Missing some fields");
//        //verify payment signature
//        boolean paymentVerificationStatus = helperUtils.verifyPaymentSignature(req.getRazorPayOrderId(), req.getRazorPayPaymentId(),
//                req.getRazorPayPaymentSignature(), orderId);
//        logger.info("Payment Verification Status => {}",paymentVerificationStatus);
//
//        //fetch order details stored in db
//        OrderEntity orderData = orderManagementRepository.getOrderById(orderId).orElseThrow(() ->
//                new RuntimeException(String.format("Cannot find order details by orderId : %s", orderId)));
//        if(paymentVerificationStatus){
//
//            //store the transaction(payment details for the order) in db
//            PaymentEntity paymentDataStored = storePaymentDetailsInDB(req, orderId);
//            //update internal orderData and save in db
//            orderData.setPaymentId(paymentDataStored.getPaymentId());
//            orderData.setOrderStatus(CommonConstants.ORDER_STATUS_PAYMENT_AUTHORISED);
//            orderData.setPaymentVerification(true);
//            OrderEntity orderDataUpdated = orderManagementRepository.save(orderData);
//            logger.info("Updated OrderData stored in db for successful payment verification :: {} ",orderDataUpdated);
//
//            //update product inventory data
//            List<OrderItemEntity> orderItems = orderItemRepository.fetchOrderItemsByOderId(orderDataUpdated.getOrderId()).orElseThrow(() ->
//                    new RuntimeException(String.format("Error While fetching orderItem list for orderId : %s", orderId)));
//            helperUtils.updateVariantInventory(orderItems);
//            logger.info("Done updating Product Variant Inventory data for orderId={}",orderId);
//
//            //todo - Order confirmed notification via sms/email
//
//            //order payment successful response
//            return new VerifyPaymentResponse(CommonConstants.SUCCESS,CommonConstants.VERIFY_PAYMENT_RESPONSE_SUCCESS_MSG,
//                    orderId,paymentDataStored.getPaymentId());
//        }else{
//            //it might be the case that payment itself is successful though the payment signature verification failed
//            //so mark it with failed verification status in db for further investigation
//            logger.error("Payment Verification failed signatures does not match for internal orderId={} with razorpay_payment_id={}",
//                    orderId,req.getRazorPayPaymentId());
//            //store the transaction(payment details for the order) in db
//            PaymentEntity paymentDataStored = storePaymentDetailsInDB(req, orderId);
//            //update internal orderData and save in db
//            orderData.setPaymentId(paymentDataStored.getPaymentId());
//            orderData.setOrderStatus(CommonConstants.ORDER_STATUS_PENDING);
//            orderData.setPaymentVerification(false);
//            OrderEntity orderDataUpdated = orderManagementRepository.save(orderData);
//            logger.info("Updated OrderData stored in db for failed payment verification :: {} ",orderDataUpdated);
//
//            //order payment verification failure response
//            return new VerifyPaymentResponse(CommonConstants.FAILURE,CommonConstants.VERIFY_PAYMENT_RESPONSE_FAILED_MSG,
//                    orderId,null);
//        }
//    }


}

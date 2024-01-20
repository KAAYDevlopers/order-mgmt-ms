package com.abw12.absolutefitness.ordermgmtms.service;

import com.abw12.absolutefitness.ordermgmtms.constants.CommonConstants;
import com.abw12.absolutefitness.ordermgmtms.constants.PaymentEventType;
import com.abw12.absolutefitness.ordermgmtms.dto.OrderDTO;
import com.abw12.absolutefitness.ordermgmtms.dto.OrderItemDTO;
import com.abw12.absolutefitness.ordermgmtms.dto.WebhookEventRes;
import com.abw12.absolutefitness.ordermgmtms.dto.request.CreateOrderReqDTO;
import com.abw12.absolutefitness.ordermgmtms.dto.response.CreateOrderResDTO;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    /**
     * @param request UI collects the userdata, product variant data from cart and calculated total amount and send to
     *                createTransactionAndPlaceOrder API.
     * @return   object containing mainly the orderId(internal use),pgOrderId received from initiate payment gateway API and few other required fields.
     */
    @Transactional
    public CreateOrderResDTO createTransactionAndPlaceOrder(CreateOrderReqDTO request) {
        logger.info("Processing the order request...");
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

        //convert order obj from payment gateway into dto and set internal orderId for ref
        CreateOrderResDTO response = CreateOrderResMapper.mapToResponse(pgOrderRes);
        response.setOrderId(storedOrderInDB.getOrderId());
        logger.info("Returning create order response to client :: {} ",response);
        return response; //return payment gateway order response to UI
    }

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

    /**
     * @param eventDataReceived Event data received from razorpay webhook api call.
     * @param receivedSignature payment signature received in handler to validated the authenticity of the payment information whether its from razorpay or not.
     */
    @Transactional
    public void handleWebhookEvent(String eventDataReceived, String receivedSignature){
        logger.info("Verifying the Event received for razorpay webhook");
        // Validate the signature
        boolean isPaymentSignatureVerified = helperUtils.verifyWebhookSignature(eventDataReceived, receivedSignature);
        logger.info("Event signature is verified={}",isPaymentSignatureVerified);
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
        if(!isPaymentSignatureVerified){
            logger.error("Payment Signature verification failed for received event");
            //set status to verification failed
            existingOrderInDB.setOrderStatus(CommonConstants.ORDER_STATUS_PAYMENT_VERIFICATION_FAILED);
            //store payment details in DB
            PaymentEntity paymentEntityStored = storePaymentDetailsInDBWebhook(orderId, entityMap);
            existingOrderInDB.setPaymentId(paymentEntityStored.getPaymentId()); //internal paymentId
            existingOrderInDB.setPaymentSignatureVerification(false);
            existingOrderInDB.setOrderModifiedAt(OffsetDateTime.now());
            //update the order details in db for failed signature verification to keep track of such orders for further analysis
            OrderEntity storedOrderData = orderManagementRepository.save(existingOrderInDB);
            // TODO: 20-01-2024 Need to decide what response should be given to client and if payment signature failed do we have to refund the amount or not
            throw new RuntimeException(String.format("Invalid Signature for webhook eventDataReceived :: Order and payment Details stored in DB for further analysis, orderId=%s",
                    storedOrderData.getOrderId()));
        }
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
            // TODO: 20-01-2024 Need to decide how Reconciliation will work to process order in CAPTURED_PENDING_AUTHORIZATION state for long time without authorized event recieved for them
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
        existingOrderInDB.setPaymentSignatureVerification(true);
        existingOrderInDB.setOrderModifiedAt(OffsetDateTime.now());
        //update the order details in db
        OrderEntity storedOrderData = orderManagementRepository.save(existingOrderInDB);
        logger.info("Successfully stored the Order & Payment data in DB for {} event received with orderId={}",eventType,orderId);
        //update product inventory data
        List<OrderItemEntity> orderItems = orderItemRepository.fetchOrderItemsByOderId(orderId).orElseThrow(() ->
                new RuntimeException(String.format("Error While fetching orderItem list for orderId : %s", orderId)));
        helperUtils.updateVariantInventoryWebhook(orderItems,eventType);
        logger.info("Done updating Product Variant Inventory data for orderId={}",orderId);
        // TODO: 20-01-2024 Need to work on response to UI
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
        existingOrderInDB.setPaymentSignatureVerification(true);
        existingOrderInDB.setOrderModifiedAt(OffsetDateTime.now());
        //update the order details in db
        OrderEntity storedOrderData = orderManagementRepository.save(existingOrderInDB);
        logger.info("Successfully stored the Order & Payment data in DB for {} event received with orderId={}",eventType,orderId);
        // TODO: 20-01-2024 Need to work on response to UI
    }

    private void markPaymentAsCaptured(OrderEntity existingOrderInDB, Map<String,Object> entityMap, String orderId, String eventType) {
        logger.info("Event Type {} is Processing...",eventType);
        existingOrderInDB.setOrderStatus(CommonConstants.ORDER_STATUS_COMPLETED);
        //store payment details in DB
        PaymentEntity paymentEntityStored = storePaymentDetailsInDBWebhook(orderId, entityMap);
        existingOrderInDB.setPaymentId(paymentEntityStored.getPaymentId());
        existingOrderInDB.setPaymentSignatureVerification(true);
        existingOrderInDB.setOrderModifiedAt(OffsetDateTime.now());
        //update the order details in db
        OrderEntity storedOrderData = orderManagementRepository.save(existingOrderInDB);
        logger.info("Successfully stored the Order & Payment data in DB for {} event received with orderId={}",eventType,orderId);
        //update product inventory data
        List<OrderItemEntity> orderItems = orderItemRepository.fetchOrderItemsByOderId(orderId).orElseThrow(() ->
                new RuntimeException(String.format("Error While fetching orderItem list for orderId : %s", orderId)));
        helperUtils.updateVariantInventoryWebhook(orderItems,eventType);
        logger.info("Done updating Product Variant Inventory data for orderId={}",orderId);
        // TODO: 20-01-2024 Need to work on response to UI and trigger email/sms notification of order confirmation
    }

    private void handlePaymentFailed(OrderEntity existingOrderInDB,Map<String,Object> entityMap,String orderId,String eventType) {
        logger.info("Event Type {} is Processing...",eventType);
        existingOrderInDB.setOrderStatus(CommonConstants.ORDER_STATUS_FAILED);
        //store payment details in DB
        PaymentEntity paymentEntityStored = storePaymentDetailsInDBWebhook(orderId, entityMap);
        existingOrderInDB.setPaymentId(paymentEntityStored.getPaymentId());
        existingOrderInDB.setPaymentSignatureVerification(true);
        existingOrderInDB.setOrderModifiedAt(OffsetDateTime.now());
        //update the order details in db
        OrderEntity storedOrderData = orderManagementRepository.save(existingOrderInDB);
        logger.info("Stored the Order & Payment data in DB for {} event received with orderId={} for further analysis",eventType,orderId);
        //update product inventory data to release the stock of reserved variant
        logger.info("Calling update inventory API to release all the variants stock on hold for orderId={}",orderId);
        List<OrderItemEntity> orderItems = orderItemRepository.fetchOrderItemsByOderId(orderId).orElseThrow(() ->
                new RuntimeException(String.format("Error While fetching orderItem list for orderId : %s", orderId)));
        helperUtils.updateVariantInventoryWebhook(orderItems,eventType);
        // TODO: 20-01-2024 Need to work on response to UI
    }

    private OrderEntity storeOrderDataInDB(OrderEntity orderData) {
        logger.info("Storing Order data with payment gateway orderId={}",orderData.getPgOrderId());
        OrderEntity storedOrderInDB = orderManagementRepository.save(orderData);
        if(StringUtils.isEmpty(storedOrderInDB.getOrderId())) throw new RuntimeException("OrderId cannot be NULL/Empty for stored order data in DB");
        logger.info("Successfully created an Order in db with Internal orderId={}  => {}",storedOrderInDB.getOrderId(),storedOrderInDB);
        return storedOrderInDB;
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
            PaymentEntity paymentData = helperUtils.preparePaymentDataWebHook( orderId, razorpayPaymentEntity);
            paymentDataStored= paymentEntityRepository.save(paymentData);
        }
        logger.info("Payment details stored successfully in db for orderId = {} => {}",orderId,paymentDataStored);
        return paymentDataStored;
    }

}

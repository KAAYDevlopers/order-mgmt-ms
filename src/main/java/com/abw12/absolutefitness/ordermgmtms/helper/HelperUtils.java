package com.abw12.absolutefitness.ordermgmtms.helper;

import com.abw12.absolutefitness.ordermgmtms.constants.CommonConstants;
import com.abw12.absolutefitness.ordermgmtms.constants.PaymentEventType;
import com.abw12.absolutefitness.ordermgmtms.dto.*;
import com.abw12.absolutefitness.ordermgmtms.dto.request.CreateOrderReqDTO;
import com.abw12.absolutefitness.ordermgmtms.dto.response.InventoryValidationRes;
import com.abw12.absolutefitness.ordermgmtms.entity.OrderEntity;
import com.abw12.absolutefitness.ordermgmtms.entity.OrderItemEntity;
import com.abw12.absolutefitness.ordermgmtms.entity.PaymentEntity;
import com.abw12.absolutefitness.ordermgmtms.gateway.interfaces.ProductCatalogClient;
import com.abw12.absolutefitness.ordermgmtms.gateway.interfaces.ProductCatalogInventoryClient;
import com.abw12.absolutefitness.ordermgmtms.mappers.OrderItemsMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class HelperUtils {

    private static final Logger logger = LoggerFactory.getLogger(HelperUtils.class);

    @Value("${payment.razorpay.api.secret}")
    private String secret;

    @Value("${payment.razorpay.webhook.secret}")
    private String webhookSecret;
    @Autowired
    private OrderItemsMapper orderItemsMapper;
    @Autowired
    private ProductCatalogInventoryClient productCatalogInventoryClient;
    @Autowired
    private ProductCatalogClient productCatalogClient;
    @Autowired
    private ObjectMapper objectMapper;


//    public boolean verifyPaymentSignature(String razorPayOrderId,String razorPayPaymentId,String razorPaymentSignature,String orderId){
//        JSONObject options = new JSONObject();
//        options.put(CommonConstants.RAZORPAY_ORDER_ID_KEY,razorPayOrderId);
//        options.put(CommonConstants.RAZORPAY_PAYMENT_ID_KEY,razorPayPaymentId);
//        options.put(CommonConstants.RAZORPAY_SIGNATURE_KEY,razorPaymentSignature);
//        boolean paymentStatus;
//        try {
//            paymentStatus= Utils.verifyPaymentSignature(options,secret);
//        } catch (RazorpayException e) {
//            logger.error("Error occurred while verifying payment signature for internal orderId={}",orderId);
//            throw new RuntimeException(e);
//        }
//        logger.info("payment signature verification is successfully");
//        return paymentStatus;
//    }

    public boolean verifyWebhookSignature(String event, String receivedSignature) {
        try {
            return Utils.verifyWebhookSignature(event,receivedSignature,webhookSecret);
        } catch (RazorpayException e) {
            logger.error("Error occurred while verifying webhook signature for event={}",event);
            throw new RuntimeException(e);
        }
    }

    public OrderEntity generateOrder(CreateOrderReqDTO req) {
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


    public PaymentEntity preparePaymentData(String orderId, Map<String,Object> razorpayPaymentEntity) {
        PaymentEntity paymentEntity =new PaymentEntity();

        if(razorpayPaymentEntity.containsKey(CommonConstants.ID))
            paymentEntity.setPgPaymentId(String.valueOf(razorpayPaymentEntity.get(CommonConstants.ID)));

        if(!StringUtils.isEmpty(orderId))
            paymentEntity.setOrderId(orderId);

        if(razorpayPaymentEntity.containsKey(CommonConstants.STATUS_KEY)
                && razorpayPaymentEntity.get(CommonConstants.STATUS_KEY) !=null )
            paymentEntity.setPaymentStatus(String.valueOf(razorpayPaymentEntity.get(CommonConstants.STATUS_KEY)));

        if(razorpayPaymentEntity.containsKey(CommonConstants.AMOUNT)
                && razorpayPaymentEntity.get(CommonConstants.AMOUNT)!=null){
            Integer amount = (Integer) razorpayPaymentEntity.get(CommonConstants.AMOUNT);
            //convert the paise from response into rupees dividing by 100
            BigDecimal convertedAmount = new BigDecimal(amount/100);
            paymentEntity.setPaymentAmount(convertedAmount);
        }
        if(razorpayPaymentEntity.containsKey(CommonConstants.RAZORPAY_PAYMENT_METHOD)
                && razorpayPaymentEntity.get(CommonConstants.RAZORPAY_PAYMENT_METHOD)!=null)
            paymentEntity.setPaymentMethod(String.valueOf(razorpayPaymentEntity.get(CommonConstants.RAZORPAY_PAYMENT_METHOD)));

        if(razorpayPaymentEntity.containsKey(CommonConstants.RAZORPAY_INVOICE_ID)
                && razorpayPaymentEntity.get(CommonConstants.RAZORPAY_INVOICE_ID)!=null)
            paymentEntity.setInvoiceId(String.valueOf(razorpayPaymentEntity.get(CommonConstants.RAZORPAY_INVOICE_ID)));

        if(razorpayPaymentEntity.containsKey(CommonConstants.RAZORPAY_REFUND_STATUS)
                && razorpayPaymentEntity.get(CommonConstants.RAZORPAY_REFUND_STATUS)!=null)
            paymentEntity.setRefundStatus(String.valueOf(razorpayPaymentEntity.get(CommonConstants.RAZORPAY_REFUND_STATUS)));

        if(razorpayPaymentEntity.containsKey(CommonConstants.RAZORPAY_AMOUNT_REFUNDED)
                && razorpayPaymentEntity.get(CommonConstants.RAZORPAY_AMOUNT_REFUNDED) !=null)
            paymentEntity.setAmountRefunded((Integer) razorpayPaymentEntity.get(CommonConstants.RAZORPAY_AMOUNT_REFUNDED));

        if(razorpayPaymentEntity.containsKey(CommonConstants.RAZORPAY_PAYMENT_CREATED_AT)
                && razorpayPaymentEntity.get(CommonConstants.RAZORPAY_PAYMENT_CREATED_AT) !=null){
            Integer timestamp = (Integer) razorpayPaymentEntity.get(CommonConstants.RAZORPAY_PAYMENT_CREATED_AT);
            OffsetDateTime createdAtDate = Instant.ofEpochSecond(timestamp.longValue())
                    .atOffset(ZoneOffset.UTC);
            paymentEntity.setPaymentCreatedDate(createdAtDate);
        }

        return paymentEntity;
    }

    public List<OrderItemEntity> generateOrderItemsList(List<OrderItemDTO> orderItemList, String orderId) {
        return orderItemList.stream().map(orderItemDTO -> {
            //Set orderId for all the orderItems with respect to same order request.
            orderItemDTO.setOrderId(orderId);
            return orderItemsMapper.dtoToEntity(orderItemDTO);
        }).toList();
    }

//    public void updateVariantInventory(List<OrderItemEntity> orderItems) {
//        orderItems.forEach(orderItem -> {
//            String variantId = orderItem.getVariantId();
//            ResponseEntity<Map<String, Objects>> variantInventoryDataRes = productCatalogInventoryClient.getVariantInventoryData(variantId);
//            VariantInventoryDTO variantInventoryData;
//            if(variantInventoryDataRes.getStatusCode().is2xxSuccessful() && variantInventoryDataRes.hasBody()){
//                variantInventoryData = objectMapper.convertValue(variantInventoryDataRes.getBody(), VariantInventoryDTO.class);
//                logger.info("Variant Inventory Data Received for variantId :: {}",variantId);
//                Long currentQuantity = variantInventoryData.getQuantity();
//                Long updatedQuantity = currentQuantity - orderItem.getQuantity();
//                variantInventoryData.setQuantity(updatedQuantity);
//                variantInventoryData.setModifiedAt(OffsetDateTime.now().toString());
//            }else{
//                throw new RuntimeException(String.format("Exception while getting variant inventory data from product-catalog-ms :: %s",
//                        variantInventoryDataRes.getStatusCode()));
//            }
//            ResponseEntity<Map<String, Objects>> updateInventoryResponse = productCatalogInventoryClient.updateInventoryData(variantInventoryData);
//            if(!updateInventoryResponse.getStatusCode().is2xxSuccessful()) {
//                logger.error("Error while updating the inventory data for variantID={} => statusCode: {}",variantId,updateInventoryResponse.getStatusCode());
//                throw new RuntimeException(String.format("Error response from update variant inventory API for variantID=%s => statusCode: %s",variantId,updateInventoryResponse.getStatusCode()));
//            }else{
//                logger.info("Successfully Updated Variant Inventory data for variantId={}",variantId);
//            }
//        });
//    }

    public ProductVariantDTO fetchVariantData(String variantId){
        logger.info("Fetching variant data for variantId={} by calling product-catalog API..",variantId);
        ResponseEntity<Map<String, Object>> response = productCatalogClient.getProductVariantById(variantId);
        ProductVariantDTO productVariantDTO;
        if(response.getStatusCode().is2xxSuccessful() && response.hasBody()){
            productVariantDTO=objectMapper.convertValue(response.getBody(),ProductVariantDTO.class);
            logger.info("Response: variant data with variantId={} => {}",variantId,productVariantDTO);
            return productVariantDTO;
        }else {
            throw new RuntimeException(String.format("Exception while getting variant data from product-catalog-ms :: %s => %s",
                    response.getStatusCode(),response.getBody()));
        }
    }

    public OrderItemHistory constructOrderHistoryItem(ProductVariantDTO variant,
                                                      OrderItemEntity orderItem)
    {
        OrderItemHistory itemHistory = new OrderItemHistory();
        if(!StringUtils.isEmpty(variant.getVariantName()))
            itemHistory.setVariantName(variant.getVariantName());
        if(!StringUtils.isEmpty(variant.getVariantValue()))
            itemHistory.setVariantValue(variant.getVariantValue());
        if(!StringUtils.isEmpty(variant.getVariantType()))
            itemHistory.setVariantType(variant.getVariantType());
        if(!StringUtils.isEmpty(variant.getImagePath()))
            itemHistory.setImagePath(variant.getImagePath());
        if(!StringUtils.isEmpty(orderItem.getProductName()))
            itemHistory.setProductName(orderItem.getProductName());
        if(orderItem.getQuantity()!=null)
            itemHistory.setQuantity(orderItem.getQuantity());
        if(orderItem.getPricePerUnit()!=null)
            itemHistory.setPricePerUnit(orderItem.getPricePerUnit());

        return itemHistory;
    }

    public void updateVariantInventoryWebhook(List<OrderItemEntity> orderItems,String eventType) {
        orderItems.forEach(orderItem -> {
            String variantId = orderItem.getVariantId();
            Map<String,Object> request =
                    switch (eventType) {
                        case PaymentEventType.CAPTURED -> updateQuantity(orderItem);
                        case PaymentEventType.FAILED -> releaseReservedStock(orderItem);
                        default -> throw new IllegalStateException(String.format("Unexpected eventType=%s received while updating the inventory data for variantId=%s ",
                                eventType,variantId));
                    };
            logger.info("Request to update variantId={} inventory data ::  Request: => {}",variantId,request);
            ResponseEntity<Map<String, Object>> updateInventoryResponse = productCatalogInventoryClient.patchVariantInventoryData(request);
            if(!updateInventoryResponse.getStatusCode().is2xxSuccessful()) {
                logger.error("Error while updating the inventory data for variantID={} :: statusCode: {} :: responseBody: {}",variantId,updateInventoryResponse.getStatusCode(),updateInventoryResponse.getBody());
                throw new RuntimeException(String.format("Error response from update variant inventory API for variantID=%s => statusCode: %s",variantId,updateInventoryResponse.getStatusCode()));
            }else{
                logger.info("Successfully Updated Variant Inventory data for variantId={}",variantId);
            }
        });
    }

    // TODO: 20-01-2024 Need to decide how reserved quantity will work in case of simultaneous request for same variant
    //  currently just set the reserved quantity and set the flag true
    private Map<String,Object> releaseReservedStock( OrderItemEntity orderItem) {
        Map<String,Object> requestBody = new HashMap<>();
        requestBody.put("variantInventoryId",orderItem.getVariantInventoryId());
        requestBody.put("releaseQuantity",orderItem.getQuantity());
        requestBody.put("isReserved","false");
        return requestBody;
    }

    private Map<String,Object> updateQuantity(OrderItemEntity orderItem){
        Map<String,Object> requestBody = new HashMap<>();
        requestBody.put("variantInventoryId",orderItem.getVariantInventoryId());
        requestBody.put("updateQuantity",orderItem.getQuantity());
        requestBody.put("releaseQuantity",orderItem.getQuantity());
        requestBody.put("isReserved","false");
        return requestBody;
    }

    public OrderStatusUpdateEvent constructOrderUpdateEvent(String orderId, OrderEntity orderEntity, PaymentEntity paymentEntity){
        OrderStatusUpdateEvent orderUpdateEvent = new OrderStatusUpdateEvent();
        if(!StringUtils.isEmpty(orderId))
            orderUpdateEvent.setOrderId(orderId);
        if(orderEntity!=null && orderEntity.getOrderNumber()!=null)
            orderUpdateEvent.setOrderNumber(orderEntity.getOrderNumber());
        if(orderEntity!=null && !StringUtils.isEmpty(orderEntity.getUserId()))
            orderUpdateEvent.setUserId(orderEntity.getUserId());
        if(paymentEntity!=null && !StringUtils.isEmpty(paymentEntity.getPaymentId()))
            orderUpdateEvent.setPaymentId(paymentEntity.getPaymentId());
        if(paymentEntity!=null && !StringUtils.isEmpty(paymentEntity.getPaymentStatus()))
            orderUpdateEvent.setPaymentStatus(paymentEntity.getPaymentStatus());

        return orderUpdateEvent;
    }

    public Boolean checkStockStatus(CreateOrderReqDTO request) {
        return request.getOrderItemList().stream()
                .allMatch(orderItem -> {
                    Map<String, Object> inventoryValidationReq = new HashMap<>();
                    if (!StringUtils.isEmpty(orderItem.getVariantId()))
                        inventoryValidationReq.put("variantId", orderItem.getVariantId());
                    if (orderItem.getQuantity() != null)
                        inventoryValidationReq.put("quantityRequested", orderItem.getQuantity());

                    ResponseEntity<Map<String, Object>> inventoryResponse = productCatalogInventoryClient.cartValidation(inventoryValidationReq);
                    if (!inventoryResponse.getStatusCode().is2xxSuccessful()) {
                        logger.error("Error while calling cart validation API of product catalog service for variantID={} => statusCode: {}", orderItem.getVariantId(), inventoryResponse.getStatusCode());
                        throw new RuntimeException(String.format("Error response from calling cart validation API of product catalog service for variantID=%s => statusCode: %s", orderItem.getVariantId(), inventoryResponse.getStatusCode()));
                    } else {
                        logger.info("Successfully fetched Inventory stock status for variantId={}", orderItem.getVariantId());
                        InventoryValidationRes inventoryValidationRes = objectMapper.convertValue(inventoryResponse.getBody(), InventoryValidationRes.class);
                        if (inventoryValidationRes.getStockStatus().equalsIgnoreCase(CommonConstants.OUT_OF_STOCK)) {
                            logger.error("Requested Quantity is OUT OF STOCK for  variantID={} => requestedQuantity={}", orderItem.getVariantId(), orderItem.getQuantity());
                            return false;
                        } else {
                            return true;
                        }
                    }
                }); //if any of the orderItem from the list is out of stock return false and cancel the order
    }

    public void reserveVariantInventory(List<OrderItemDTO> storedOrderItems) {
        storedOrderItems.forEach(orderItem -> {
            Map<String,Object> params = new HashMap<>();
            if(!StringUtils.isEmpty(orderItem.getVariantInventoryId()))
                params.put("variantInventoryId",orderItem.getVariantInventoryId());
            if(orderItem.getQuantity()!=null)
                params.put("reservedQuantity",String.valueOf(orderItem.getQuantity()));

            params.put("isReserved","true");

            ResponseEntity<Map<String, Object>> response = productCatalogInventoryClient.patchVariantInventoryData(params);
            if(response.getStatusCode().is2xxSuccessful()){
                logger.info("Successfully updated variant inventory data for variantId={} with reserved quantity of :: {}",orderItem.getVariantId(),orderItem.getQuantity());
            }else {
                logger.error("Error occurred while updating variant inventory data for variantID={} => statusCode: {} => body: {}",orderItem.getVariantId(),response.getStatusCode(),response.getBody());
            }
        });
    }

    public String offsetDateTimeFormatter(OffsetDateTime dateField){
        if(dateField==null) return "";
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
        return dateField.format(fmt);
    }

    public static DateTimeFormatter dateFormat(){
        return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    }
}


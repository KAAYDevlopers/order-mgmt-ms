package com.abw12.absolutefitness.ordermgmtms.helper;

import com.abw12.absolutefitness.ordermgmtms.constants.CommonConstants;
import com.abw12.absolutefitness.ordermgmtms.constants.PaymentEventType;
import com.abw12.absolutefitness.ordermgmtms.dto.*;
import com.abw12.absolutefitness.ordermgmtms.dto.request.CreateOrderReqDTO;
import com.abw12.absolutefitness.ordermgmtms.dto.request.VariantInventoryDTO;
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
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

//    public PaymentEntity preparePaymentData(String razorpayPaymentId, String orderId, Payment paymentDetails) {
//        PaymentEntity paymentEntity =new PaymentEntity();
//
//        if(!razorpayPaymentId.isEmpty())
//            paymentEntity.setPgPaymentId(razorpayPaymentId);
//
//        if(StringUtils.isEmpty(orderId))
//            paymentEntity.setOrderId(orderId);
//
//        if(paymentDetails.has(CommonConstants.STATUS_KEY)
//                && !StringUtils.isEmpty(paymentDetails.get(CommonConstants.STATUS_KEY)))
//            paymentEntity.setPaymentStatus(paymentDetails.get(CommonConstants.STATUS_KEY));
//
//        if(paymentDetails.has(CommonConstants.AMOUNT)
//                && paymentDetails.get(CommonConstants.AMOUNT)!=null){
//            Integer amount = paymentDetails.get(CommonConstants.AMOUNT);
//            //convert the paise from response into rupees dividing by 100
//            BigDecimal convertedAmount = new BigDecimal(amount/100);
//            paymentEntity.setPaymentAmount(convertedAmount);
//        }
//        if(paymentDetails.has(CommonConstants.RAZORPAY_PAYMENT_METHOD)
//                && !StringUtils.isEmpty(paymentDetails.get(CommonConstants.RAZORPAY_PAYMENT_METHOD)))
//            paymentEntity.setPaymentMethod(paymentDetails.get(CommonConstants.RAZORPAY_PAYMENT_METHOD));
//
//        if(paymentDetails.has(CommonConstants.RAZORPAY_INVOICE_ID)
//                && !StringUtils.isEmpty(paymentDetails.get(CommonConstants.RAZORPAY_INVOICE_ID)))
//            paymentEntity.setInvoiceId(paymentDetails.get(CommonConstants.RAZORPAY_INVOICE_ID));
//
//        if(paymentDetails.has(CommonConstants.RAZORPAY_REFUND_STATUS)
//                && !StringUtils.isEmpty(paymentDetails.get(CommonConstants.RAZORPAY_REFUND_STATUS)))
//            paymentEntity.setRefundStatus(paymentDetails.get(CommonConstants.RAZORPAY_REFUND_STATUS));
//
//        if(paymentDetails.has(CommonConstants.RAZORPAY_AMOUNT_REFUNDED)
//                && paymentDetails.get(CommonConstants.RAZORPAY_AMOUNT_REFUNDED) !=null)
//            paymentEntity.setAmountRefunded(paymentDetails.get(CommonConstants.RAZORPAY_AMOUNT_REFUNDED));
//
//        if(paymentDetails.has(CommonConstants.RAZORPAY_PAYMENT_CREATED_AT)
//                && paymentDetails.get(CommonConstants.RAZORPAY_PAYMENT_CREATED_AT) !=null)
//            paymentEntity.setPaymentCreatedDate(
//                    OffsetDateTime.parse(
//                            String.valueOf(paymentDetails.get(CommonConstants.RAZORPAY_PAYMENT_CREATED_AT))
//                    )
//            );
//
//        return paymentEntity;
//    }

    public PaymentEntity preparePaymentDataWebHook(String orderId, Map<String,Object> razorpayPaymentEntity) {
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
                && razorpayPaymentEntity.get(CommonConstants.RAZORPAY_PAYMENT_CREATED_AT) !=null)
            paymentEntity.setPaymentCreatedDate(
                    OffsetDateTime.parse(
                            String.valueOf(razorpayPaymentEntity.get(CommonConstants.RAZORPAY_PAYMENT_CREATED_AT))
                    )
            );

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
        if(orderItem.getQuantity()!=null)
            itemHistory.setQuantity(orderItem.getQuantity());
        if(orderItem.getPricePerUnit()!=null)
            itemHistory.setPricePerUnit(orderItem.getPricePerUnit());
        return itemHistory;
    }

    public void updateVariantInventoryWebhook(List<OrderItemEntity> orderItems,String eventType) {
        orderItems.forEach(orderItem -> {
            String variantId = orderItem.getVariantId();
            ResponseEntity<Map<String, Objects>> variantInventoryDataRes = productCatalogInventoryClient.getVariantInventoryData(variantId);
            VariantInventoryDTO updatedVariantInventoryData;
            if(variantInventoryDataRes.getStatusCode().is2xxSuccessful() && variantInventoryDataRes.hasBody()){
                VariantInventoryDTO parsedVariantData = objectMapper.convertValue(variantInventoryDataRes.getBody(), VariantInventoryDTO.class);
                logger.info("Variant Inventory Data Received for variantId :: {}",variantId);
                updatedVariantInventoryData=
                        switch (eventType) {
                            case PaymentEventType.AUTHORIZED -> reserveQuantity(parsedVariantData, orderItem,variantId);
                            case PaymentEventType.CAPTURED -> updateQuantity(parsedVariantData,orderItem);
                            case PaymentEventType.FAILED -> releaseReservedStock(parsedVariantData,variantId);
                            default -> throw new IllegalStateException(String.format("Unexpected eventType=%s received while updating the inventory data for variantId=%s ",
                                    eventType,variantId));
                        };

            }else{
                throw new RuntimeException(String.format("Exception while getting variant inventory data from product-catalog-ms :: %s => %s",
                        variantInventoryDataRes.getStatusCode(),variantInventoryDataRes.getBody()));
            }
            logger.info("Request to update variantId={} inventory data ::  Request: => {}",variantId,updatedVariantInventoryData);
            ResponseEntity<Map<String, Objects>> updateInventoryResponse = productCatalogInventoryClient.updateInventoryData(updatedVariantInventoryData);
            if(!updateInventoryResponse.getStatusCode().is2xxSuccessful()) {
                logger.error("Error while updating the inventory data for variantID={} => statusCode: {}",variantId,updateInventoryResponse.getStatusCode());
                throw new RuntimeException(String.format("Error response from update variant inventory API for variantID=%s => statusCode: %s",variantId,updateInventoryResponse.getStatusCode()));
            }else{
                logger.info("Successfully Updated Variant Inventory data for variantId={}",variantId);
            }
        });
    }

    private VariantInventoryDTO releaseReservedStock(VariantInventoryDTO variantInventoryData, String variantId) {
        variantInventoryData.setReserved(false);
        variantInventoryData.setReservedQuantity(0L);
        variantInventoryData.setModifiedAt(OffsetDateTime.now().toString());
        logger.info("released stock for variantId={}",variantId);
        return variantInventoryData;
    }

    private VariantInventoryDTO reserveQuantity(VariantInventoryDTO variantInventoryData, OrderItemEntity orderItem,String variantId){
        // TODO: 20-01-2024 Need to decide how reserved quantity will work in case of simultaneous request for same variant
        //  currently just set the reserved quantity and set the flag true
        Long currentQuantity = variantInventoryData.getQuantity();
        if(orderItem.getQuantity() > currentQuantity){
            logger.info("Current Inventory stock is less than requested quantity for the variantId={} => requestQuantity={}",
                                     variantId,orderItem.getQuantity());
            throw new RuntimeException(String.format("Current Inventory stock is less than requested quantity for the variantId=%s => requestQuantity=%s",
                    variantId,orderItem.getQuantity()));
        }
        variantInventoryData.setReserved(true);
        variantInventoryData.setReservedQuantity(orderItem.getQuantity());
        variantInventoryData.setModifiedAt(OffsetDateTime.now().toString());
        return variantInventoryData;
    }

    private VariantInventoryDTO updateQuantity(VariantInventoryDTO variantInventoryData,OrderItemEntity orderItem){
        Long currentQuantity = orderItem.getQuantity();
        Long updatedQuantity = currentQuantity - orderItem.getQuantity();
        variantInventoryData.setQuantity(updatedQuantity);
        variantInventoryData.setReserved(false);
        variantInventoryData.setReservedQuantity(0L);
        variantInventoryData.setModifiedAt(OffsetDateTime.now().toString());
        return variantInventoryData;
    }
}


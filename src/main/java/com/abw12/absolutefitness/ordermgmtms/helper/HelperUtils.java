package com.abw12.absolutefitness.ordermgmtms.helper;

import com.abw12.absolutefitness.ordermgmtms.constants.CommonConstants;
import com.abw12.absolutefitness.ordermgmtms.dto.OrderItemDTO;
import com.abw12.absolutefitness.ordermgmtms.dto.UserDataDTO;
import com.abw12.absolutefitness.ordermgmtms.dto.request.CreateOrderReqDTO;
import com.abw12.absolutefitness.ordermgmtms.dto.request.VariantInventoryDTO;
import com.abw12.absolutefitness.ordermgmtms.entity.OrderEntity;
import com.abw12.absolutefitness.ordermgmtms.entity.OrderItemEntity;
import com.abw12.absolutefitness.ordermgmtms.entity.PaymentEntity;
import com.abw12.absolutefitness.ordermgmtms.gateway.interfaces.ProductCatalogInventoryClient;
import com.abw12.absolutefitness.ordermgmtms.mappers.OrderItemsMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorpay.Payment;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
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
    @Autowired
    private OrderItemsMapper orderItemsMapper;
    @Autowired
    private ProductCatalogInventoryClient productCatalogInventoryClient;
    @Autowired
    private ObjectMapper objectMapper;


    public boolean verifyPaymentSignature(String razorPayOrderId,String razorPayPaymentId,String razorPaymentSignature,String orderId){
        JSONObject options = new JSONObject();
        options.put(CommonConstants.RAZORPAY_ORDER_ID_KEY,razorPayOrderId);
        options.put(CommonConstants.RAZORPAY_PAYMENT_ID_KEY,razorPayPaymentId);
        options.put(CommonConstants.RAZORPAY_SIGNATURE_KEY,razorPaymentSignature);
        boolean paymentStatus;
        try {
            paymentStatus= Utils.verifyPaymentSignature(options,secret);
        } catch (RazorpayException e) {
            logger.error("Error occurred while verifying payment signature for internal orderId={}",orderId);
            throw new RuntimeException(e);
        }
        logger.info("payment signature verification is successfully");
        return paymentStatus;
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

    public PaymentEntity preparePaymentData(String razorpayPaymentId, String orderId, Payment paymentDetails) {
        PaymentEntity paymentEntity =new PaymentEntity();

        if(!razorpayPaymentId.isEmpty())
            paymentEntity.setPgPaymentId(razorpayPaymentId);

        if(StringUtils.isEmpty(orderId))
            paymentEntity.setOrderId(orderId);

        if(paymentDetails.has(CommonConstants.STATUS_KEY)
                && !StringUtils.isEmpty(paymentDetails.get(CommonConstants.STATUS_KEY)))
            paymentEntity.setPaymentStatus(paymentDetails.get(CommonConstants.STATUS_KEY));

        if(paymentDetails.has(CommonConstants.AMOUNT)
                && paymentDetails.get(CommonConstants.AMOUNT)!=null){
            Integer amount = paymentDetails.get(CommonConstants.AMOUNT);
            //convert the paise from response into rupees dividing by 100
            BigDecimal convertedAmount = new BigDecimal(amount/100);
            paymentEntity.setPaymentAmount(convertedAmount);
        }
        if(paymentDetails.has(CommonConstants.RAZORPAY_PAYMENT_METHOD)
                && !StringUtils.isEmpty(paymentDetails.get(CommonConstants.RAZORPAY_PAYMENT_METHOD)))
            paymentEntity.setPaymentMethod(paymentDetails.get(CommonConstants.RAZORPAY_PAYMENT_METHOD));

        if(paymentDetails.has(CommonConstants.RAZORPAY_INVOICE_ID)
            && !StringUtils.isEmpty(paymentDetails.get(CommonConstants.RAZORPAY_INVOICE_ID)))
                paymentEntity.setInvoiceId(paymentDetails.get(CommonConstants.RAZORPAY_INVOICE_ID));

        if(paymentDetails.has(CommonConstants.RAZORPAY_REFUND_STATUS)
                && !StringUtils.isEmpty(paymentDetails.get(CommonConstants.RAZORPAY_REFUND_STATUS)))
            paymentEntity.setRefundStatus(paymentDetails.get(CommonConstants.RAZORPAY_REFUND_STATUS));

        if(paymentDetails.has(CommonConstants.RAZORPAY_AMOUNT_REFUNDED)
                && paymentDetails.get(CommonConstants.RAZORPAY_AMOUNT_REFUNDED) !=null)
            paymentEntity.setAmountRefunded(paymentDetails.get(CommonConstants.RAZORPAY_AMOUNT_REFUNDED));

        if(paymentDetails.has(CommonConstants.RAZORPAY_PAYMENT_CREATED_AT)
                && paymentDetails.get(CommonConstants.RAZORPAY_PAYMENT_CREATED_AT) !=null)
            paymentEntity.setPaymentCreatedDate(
                    OffsetDateTime.parse(
                        String.valueOf(paymentDetails.get(CommonConstants.RAZORPAY_PAYMENT_CREATED_AT))
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

    public void updateVariantInventory(List<OrderItemEntity> orderItems) {
        orderItems.forEach(orderItem -> {
            String variantId = orderItem.getVariantId();
            ResponseEntity<Map<String, Objects>> variantInventoryDataRes = productCatalogInventoryClient.getVariantInventoryData(variantId);
            VariantInventoryDTO variantInventoryData;
            if(variantInventoryDataRes.getStatusCode().is2xxSuccessful() && variantInventoryDataRes.hasBody()){
                variantInventoryData = objectMapper.convertValue(variantInventoryDataRes.getBody(), VariantInventoryDTO.class);
                logger.info("Variant Inventory Data Received for variantId :: {}",variantId);
                Long currentQuantity = variantInventoryData.getQuantity();
                Long updatedQuantity = currentQuantity - orderItem.getQuantity();
                variantInventoryData.setQuantity(updatedQuantity);
                variantInventoryData.setModifiedAt(OffsetDateTime.now().toString());
            }else{
                throw new RuntimeException(String.format("Exception while getting variant inventory data from product-catalog-ms :: %s",
                        variantInventoryDataRes.getStatusCode()));
            }
            ResponseEntity<Map<String, Objects>> updateInventoryResponse = productCatalogInventoryClient.updateInventoryData(variantInventoryData);
            if(!updateInventoryResponse.getStatusCode().is2xxSuccessful()) {
                logger.error("Error while updating the inventory data for variantID={} => statusCode: {}",variantId,updateInventoryResponse.getStatusCode());
                throw new RuntimeException(String.format("Error response from update variant inventory API for variantID=%s => statusCode: %s",variantId,updateInventoryResponse.getStatusCode()));
            }else{
                logger.info("Successfully Updated Variant Inventory data for variantId={}",variantId);
            }
        });
    }
}


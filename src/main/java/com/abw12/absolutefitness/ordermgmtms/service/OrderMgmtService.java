package com.abw12.absolutefitness.ordermgmtms.service;

import com.abw12.absolutefitness.ordermgmtms.constants.CommonConstants;
import com.abw12.absolutefitness.ordermgmtms.dto.OrderItemDTO;
import com.abw12.absolutefitness.ordermgmtms.dto.request.CreateOrderReqDTO;
import com.abw12.absolutefitness.ordermgmtms.dto.request.VerifyPaymentReqDTO;
import com.abw12.absolutefitness.ordermgmtms.dto.response.CreateOrderResDTO;
import com.abw12.absolutefitness.ordermgmtms.dto.response.VerifyPaymentResponse;
import com.abw12.absolutefitness.ordermgmtms.entity.OrderEntity;
import com.abw12.absolutefitness.ordermgmtms.entity.OrderItemEntity;
import com.abw12.absolutefitness.ordermgmtms.entity.PaymentEntity;
import com.abw12.absolutefitness.ordermgmtms.gateway.implementation.PaymentGatewayRestImpl;
import com.abw12.absolutefitness.ordermgmtms.helper.HelperUtils;
import com.abw12.absolutefitness.ordermgmtms.mappers.CreateOrderResMapper;
import com.abw12.absolutefitness.ordermgmtms.mappers.OrderItemsMapper;
import com.abw12.absolutefitness.ordermgmtms.repository.OrderItemRepository;
import com.abw12.absolutefitness.ordermgmtms.repository.OrderManagementRepository;
import com.abw12.absolutefitness.ordermgmtms.repository.PaymentEntityRepository;
import com.razorpay.Order;
import com.razorpay.Payment;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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
    private OrderItemsMapper orderItemsMapper;
    @Autowired
    private HelperUtils helperUtils;


    private static final Logger logger = LoggerFactory.getLogger(OrderMgmtService.class);

    /**
     * @param request UI collects the userdata, product variant data from cart and calculated total amount and send to
     *                createTransactionAndPlaceOrder API.
     * @return   object containing mainly the orderId(internal use),pgOrderId received from initiate payment gateway API and few other required fields.
     */
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

    @Transactional
    public VerifyPaymentResponse verifyPaymentWithOrderId(VerifyPaymentReqDTO req , String orderId){
        if(req == null ||
                StringUtils.isEmpty(req.getRazorPayOrderId())
                        && StringUtils.isEmpty(req.getRazorPayPaymentId())
                        && StringUtils.isEmpty(req.getRazorPayPaymentSignature())
        ) throw new RuntimeException("Invalid Request VerifyPaymentReq is NULL or Missing some fields");

        boolean paymentVerificationStatus = helperUtils.verifyPaymentSignature(req.getRazorPayOrderId(), req.getRazorPayPaymentId(),
                req.getRazorPayPaymentSignature(), orderId);
        logger.info("Payment Verification Status => {}",paymentVerificationStatus);
        //fetch order details stored in db
        OrderEntity orderData = orderManagementRepository.getOrderById(orderId).orElseThrow(() ->
                new RuntimeException(String.format("Cannot find order details by orderId : %s", orderId)));
        if(paymentVerificationStatus){

            //store the transaction(payment details for the order) in db
            PaymentEntity paymentDataStored = storePaymentDetailsInDB(req, orderId);
            //update internal orderData and save in db
            orderData.setPaymentId(paymentDataStored.getPaymentId());
            orderData.setOrderStatus(CommonConstants.ORDER_STATUS_CONFIRMED);
            orderData.setPaymentVerification(true);
            OrderEntity orderDataUpdated = orderManagementRepository.save(orderData);
            logger.info("Updated OrderData stored in db for successful payment verification :: {} ",orderDataUpdated);

            //update product inventory data
            List<OrderItemEntity> orderItems = orderItemRepository.fetchOrderItemsByOderId(orderDataUpdated.getOrderId()).orElseThrow(() ->
                    new RuntimeException(String.format("Error While fetching orderItem list for orderId : %s", orderId)));
            helperUtils.updateVariantInventory(orderItems);
            logger.info("Done updating Product Variant Inventory data for orderId={}",orderId);

            //todo - Order confirmed notification via sms

            //order payment successful response
            return new VerifyPaymentResponse(CommonConstants.SUCCESS,CommonConstants.VERIFY_PAYMENT_RESPONSE_SUCCESS_MSG,
                    orderId,paymentDataStored.getPaymentId());
        }else{
            //it might be the case that payment itself is successful though the payment signature verification failed
            //so mark it with failed verification status in db for further investigation
            logger.error("Payment Verification failed signatures does not match for internal orderId={} with razorpay_payment_id={}",
                    orderId,req.getRazorPayPaymentId());
            //store the transaction(payment details for the order) in db
            PaymentEntity paymentDataStored = storePaymentDetailsInDB(req, orderId);
            //update internal orderData and save in db
            orderData.setPaymentId(paymentDataStored.getPaymentId());
            orderData.setOrderStatus(CommonConstants.ORDER_STATUS_PENDING);
            orderData.setPaymentVerification(false);
            OrderEntity orderDataUpdated = orderManagementRepository.save(orderData);
            logger.info("Updated OrderData stored in db for failed payment verification :: {} ",orderDataUpdated);

            //order payment verification failure response
            return new VerifyPaymentResponse(CommonConstants.FAILURE,CommonConstants.VERIFY_PAYMENT_RESPONSE_FAILED_MSG,
                    orderId,null);
        }
    }



    private OrderEntity storeOrderDataInDB(OrderEntity orderData) {
        logger.info("Storing Order data with payment gateway orderId={}",orderData.getPgOrderId());
        OrderEntity storedOrderInDB = orderManagementRepository.save(orderData);
        if(StringUtils.isEmpty(storedOrderInDB.getOrderId())) throw new RuntimeException("OrderId cannot be NULL/Empty for stored order data in DB");
        logger.info("Successfully created an Order in db with Internal orderId={}  => {}",storedOrderInDB.getOrderId(),storedOrderInDB);
        return storedOrderInDB;
    }

    private PaymentEntity storePaymentDetailsInDB(VerifyPaymentReqDTO req,String orderId){
        //fetch payment details from the razorpay payment gateway
        Payment paymentDetailsFetched = paymentGatewayRest.fetchPaymentDetails(req.getRazorPayPaymentId());

        //store payment details in db
        PaymentEntity paymentData = helperUtils.preparePaymentData(req.getRazorPayPaymentId(), orderId, paymentDetailsFetched);
        PaymentEntity paymentDataStored = paymentEntityRepository.save(paymentData);
        logger.info("Payment details stored successfully in db for orderId = {} => {}",orderId,paymentDataStored);
        return paymentDataStored;
    }


}

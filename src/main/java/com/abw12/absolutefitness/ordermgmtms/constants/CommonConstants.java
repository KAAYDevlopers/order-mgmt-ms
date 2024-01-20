package com.abw12.absolutefitness.ordermgmtms.constants;

import org.springframework.stereotype.Component;

@Component
public class CommonConstants {
    public static final String PRODUCT_ID="productId";
    public static final String VARIANT_ID="variantId";

    public static final String IN_STOCK ="InStock";

    public static final String OUT_OF_STOCK="OutOfStock";

    public static final String CURRENCY_VALUE_INR="INR";
    public static final String CURRENCY_KEY="currency";
    public static final String ORDER_STATUS_PENDING="PENDING";
    public static final String ORDER_STATUS_CAPTURED_PENDING_AUTHORIZATION="CAPTURED_PENDING_AUTHORIZATION";
    public static final String ORDER_STATUS_COMPLETED="COMPLETED"; //when payment order.paid event is receieved
    public static final String ORDER_STATUS_PAYMENT_AUTHORIZED ="PAYMENT_AUTHORIZED"; //when payment is authorised
    public static final String ORDER_STATUS_PAYMENT_VERIFICATION_FAILED ="PAYMENT_VERIFICATION_FAILED"; //signature verification failed
    public static final String ORDER_STATUS_FAILED="FAILED"; //when payment failed
    public static final String ORDER_STATUS_PAYMENT_CAPTURED="CAPTURED"; //when payment captured event is received
    public static final String SUCCESS = "success";
    public static final String FAILURE = "failure";
    public static final String VERIFY_PAYMENT_RESPONSE_SUCCESS_MSG="Payment verification successful. Order confirmed.";
    public static final String VERIFY_PAYMENT_RESPONSE_FAILED_MSG="Payment verification failed. Please try again or contact support.";
    public static final String AMOUNT="amount";
    public static final String RECEIPT="receipt";
    public static final String NOTES="notes";
    public static final String ID = "id";
    public static final String STATUS_KEY = "status";
    public static final String RAZORPAY_ORDER_ID_KEY = "razorpay_order_id";
    public static final String RAZORPAY_PAYMENT_ID_KEY = "razorpay_payment_id";
    public static final String RAZORPAY_SIGNATURE_KEY = "razorpay_signature";
    public static final String RAZORPAY_INVOICE_ID = "invoice_id";
    public static final String RAZORPAY_ORDER_ID = "order_id";
    public static final String RAZORPAY_AMOUNT_REFUNDED = "amount_refunded";
    public static final String RAZORPAY_REFUND_STATUS = "refund_status";
    public static final String RAZORPAY_PAYMENT_METHOD = "method";
    public static final String RAZORPAY_PAYMENT_CREATED_AT = "created_at";

}

package com.abw12.absolutefitness.ordermgmtms.constants;

import org.springframework.stereotype.Component;

@Component
public class PaymentEventType {

    public static final String AUTHORIZED ="payment.authorized";  //It's a confirmation that the funds are available, but they haven't been transferred yet.
    public static final String CAPTURED="payment.captured"; //the funds have been transferred from the customer's account to merchant account.
    public static final String FAILED="payment.failed"; //this event occurs when a payment attempt fails, either due to insufficient funds, incorrect payment details, or other processing issues.



}

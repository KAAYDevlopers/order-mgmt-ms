package com.abw12.absolutefitness.ordermgmtms.advice;

public class InvalidDataRequestException extends RuntimeException{
    public InvalidDataRequestException(String errorMsg ){
        super(errorMsg);
    }
}

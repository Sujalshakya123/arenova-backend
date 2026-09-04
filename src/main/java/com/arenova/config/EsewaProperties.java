package com.arenova.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class EsewaProperties {

    @Value("${esewa.merchant-code:EPAYTEST}")
    private String merchantCode;

    @Value("${esewa.secret-key:8gBm/:&EnhH.1/q}")
    private String secretKey;

    @Value("${esewa.payment-url:https://rc-epay.esewa.com.np/api/epay/main/v2/form}")
    private String paymentUrl;

    @Value("${esewa.status-url:https://uat.esewa.com.np/api/epay/transaction/status/}")
    private String statusUrl;

    @Value("${esewa.success-url:http://localhost:5173/payment/esewa/success}")
    private String successUrl;

    @Value("${esewa.failure-url:http://localhost:5173/payment/esewa/failure}")
    private String failureUrl;
}

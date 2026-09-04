package com.arenova.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EsewaPaymentInitDTO {

    private String paymentUrl;
    private String amount;
    private String taxAmount;
    private String totalAmount;
    private String transactionUuid;
    private String productCode;
    private String productServiceCharge;
    private String productDeliveryCharge;
    private String successUrl;
    private String failureUrl;
    private String signedFieldNames;
    private String signature;
}

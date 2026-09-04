package com.arenova.services;

import com.arenova.dtos.EsewaPaymentInitDTO;
import com.arenova.dtos.EsewaVerifyRequest;
import com.arenova.dtos.PaymentReceiptDTO;
import com.arenova.entities.EventRegistration;

import java.util.Map;

public interface PaymentService {

    EsewaPaymentInitDTO initiateEsewaForRegistration(EventRegistration registration, int amountNpr);

    EsewaPaymentInitDTO resumeEsewaPayment(Long registrationId) throws org.apache.coyote.BadRequestException;

    Map<String, Object> verifyEsewaCallback(EsewaVerifyRequest request) throws org.apache.coyote.BadRequestException;

    String paymentStatusForRegistration(EventRegistration registration);

    PaymentReceiptDTO getMyReceiptById(Long paymentId);

    PaymentReceiptDTO getMyReceiptByRegistration(Long registrationId);
}

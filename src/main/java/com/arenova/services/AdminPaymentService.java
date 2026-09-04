package com.arenova.services;

import com.arenova.dtos.AdminPaymentsOverviewDTO;
import com.arenova.dtos.PaymentReceiptDTO;

public interface AdminPaymentService {

    AdminPaymentsOverviewDTO getPaymentsOverview();

    PaymentReceiptDTO getPaymentReceipt(Long paymentId);
}

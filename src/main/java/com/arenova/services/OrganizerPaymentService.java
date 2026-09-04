package com.arenova.services;

import com.arenova.dtos.OrganizerPaymentsOverviewDTO;
import com.arenova.dtos.PaymentReceiptDTO;

public interface OrganizerPaymentService {

    OrganizerPaymentsOverviewDTO getEventPaymentsOverview(Long eventId);

    PaymentReceiptDTO getEventPaymentReceipt(Long eventId, Long paymentId);
}

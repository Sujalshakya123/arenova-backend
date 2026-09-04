package com.arenova.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterEventResponseDTO {

    private EventRegistrationDTO registration;

    /** Present when entry fee > 0 and paymentMethod is esewa. */
    private EsewaPaymentInitDTO esewaPayment;
}

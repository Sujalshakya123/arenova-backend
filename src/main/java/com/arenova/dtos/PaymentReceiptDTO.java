package com.arenova.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentReceiptDTO {

    private Long id;
    private Long registrationId;
    private Long eventId;
    private String tournament;
    private String playerName;
    private String email;
    private String amount;
    private String method;
    /** Completed | Pending | Failed */
    private String status;
    private String transactionUuid;
    private String esewaRefId;
    private String paidAt;
    private String createdAt;
}

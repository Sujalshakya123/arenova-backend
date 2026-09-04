package com.arenova.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminPaymentDTO {

    private Long id;
    private String playerName;
    private String email;
    private String tournament;
    private String amount;
    private String method;
    private String date;
    /** Completed | Pending | Failed */
    private String status;
}

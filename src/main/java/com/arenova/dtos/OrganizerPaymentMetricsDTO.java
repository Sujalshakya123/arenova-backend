package com.arenova.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizerPaymentMetricsDTO {

    private String totalRevenue;
    private String platformCommission;
    private String organizerShare;
    private String paidEntries;
}

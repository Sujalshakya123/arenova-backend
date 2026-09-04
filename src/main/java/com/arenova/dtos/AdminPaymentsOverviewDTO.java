package com.arenova.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminPaymentsOverviewDTO {

    private AdminPaymentMetricsDTO metrics;
    private List<AdminPaymentDTO> payments;
}

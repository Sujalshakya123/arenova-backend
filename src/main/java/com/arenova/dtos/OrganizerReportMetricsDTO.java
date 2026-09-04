package com.arenova.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizerReportMetricsDTO {

    private String collectedAmount;
    private String commission;
    private String prize;
    private String sales;
}

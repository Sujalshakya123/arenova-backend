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
public class AdminSettlementsOverviewDTO {

    private AdminSettlementMetricsDTO metrics;
    private List<AdminSettlementDTO> settlements;
    private List<AdminSettlementOrganizerOptionDTO> organizers;
}

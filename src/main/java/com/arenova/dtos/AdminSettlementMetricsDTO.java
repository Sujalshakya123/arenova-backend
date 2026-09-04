package com.arenova.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminSettlementMetricsDTO {

    private int settledTournaments;
    private int pendingApprovals;
    private String totalRevenue;
    private String platformCommission;
    private String organizerPayouts;
    private String playerPrizePool;
}

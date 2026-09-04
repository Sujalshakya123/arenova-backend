package com.arenova.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardStatsDTO {

    private long totalUsers;
    private long totalOrganizers;
    private long totalTournaments;
    private long activeTournaments;
    private long pendingTournamentApprovals;
    private long pendingOrganizers;
    /** Gross sum of all COMPLETED player payments (GMV). */
    private String totalRevenue;
    /** Estimated platform share = 10% of all completed payments (includes unsettled). */
    private String platformCommission;
    /** Total revenue from APPROVED / COMPLETED settlements only. */
    private String settledRevenue;
    /** Platform earnings from APPROVED / COMPLETED settlements only (10%). */
    private String settledPlatformEarnings;
}

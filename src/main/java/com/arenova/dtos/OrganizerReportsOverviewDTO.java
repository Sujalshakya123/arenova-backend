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
public class OrganizerReportsOverviewDTO {

    private OrganizerReportMetricsDTO summary;
    private List<OrganizerReportRowDTO> rows;
    private String fromDate;
    private String toDate;
    private boolean includeAllTournaments;
    private int totalTournaments;
    private int tournamentsWithRevenue;
}

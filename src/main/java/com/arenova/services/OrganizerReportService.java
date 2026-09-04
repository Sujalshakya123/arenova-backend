package com.arenova.services;

import com.arenova.dtos.OrganizerReportsOverviewDTO;

import java.time.LocalDate;

public interface OrganizerReportService {

    OrganizerReportsOverviewDTO getReports(
            LocalDate fromDate,
            LocalDate toDate,
            boolean includeAllTournaments,
            String settlementStatusFilter
    );
}

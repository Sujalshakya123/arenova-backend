package com.arenova.controllers;

import com.arenova.dtos.OrganizerReportsOverviewDTO;
import com.arenova.services.OrganizerReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/organizer/reports")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class OrganizerReportController {

    private final OrganizerReportService organizerReportService;

    @GetMapping
    public ResponseEntity<OrganizerReportsOverviewDTO> getReports(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "false") boolean includeAll,
            @RequestParam(required = false) String settlementStatus
    ) {
        return ResponseEntity.ok(
                organizerReportService.getReports(fromDate, toDate, includeAll, settlementStatus)
        );
    }
}

package com.arenova.controllers;

import com.arenova.dtos.AdminActivityDTO;
import com.arenova.dtos.AdminDashboardStatsDTO;
import com.arenova.dtos.AdminGrowthPointDTO;
import com.arenova.services.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/stats")
    public ResponseEntity<AdminDashboardStatsDTO> getStats() {
        return ResponseEntity.ok(adminDashboardService.getStats());
    }

    @GetMapping("/activity")
    public ResponseEntity<List<AdminActivityDTO>> getRecentActivity() {
        return ResponseEntity.ok(adminDashboardService.getRecentActivity());
    }

    @GetMapping("/growth")
    public ResponseEntity<List<AdminGrowthPointDTO>> getGrowthOverview(
            @RequestParam(defaultValue = "30") int days
    ) {
        return ResponseEntity.ok(adminDashboardService.getGrowthOverview(days));
    }
}

package com.arenova.services;

import com.arenova.dtos.AdminActivityDTO;
import com.arenova.dtos.AdminDashboardStatsDTO;
import com.arenova.dtos.AdminGrowthPointDTO;

import java.util.List;

public interface AdminDashboardService {

    AdminDashboardStatsDTO getStats();

    List<AdminActivityDTO> getRecentActivity();

    List<AdminGrowthPointDTO> getGrowthOverview(int days);
}

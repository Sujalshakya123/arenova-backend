package com.arenova.services;

import com.arenova.dtos.AdminSettlementDTO;
import com.arenova.dtos.AdminSettlementsOverviewDTO;
import org.apache.coyote.BadRequestException;

public interface AdminSettlementService {

    AdminSettlementsOverviewDTO getSettlementsOverview(String type, Long organizerId);

    AdminSettlementsOverviewDTO getSettlementsOverview();

    AdminSettlementDTO approveSettlement(Long settlementId) throws BadRequestException;

    AdminSettlementDTO rejectSettlement(Long settlementId, String reason) throws BadRequestException;
}

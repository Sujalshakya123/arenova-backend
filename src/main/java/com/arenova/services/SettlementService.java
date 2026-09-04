package com.arenova.services;

import com.arenova.dtos.SettlementDTO;
import org.apache.coyote.BadRequestException;

public interface SettlementService {

    SettlementDTO getSettlement(Long eventId);

    SettlementDTO initiateSettlement(Long eventId) throws BadRequestException;
}

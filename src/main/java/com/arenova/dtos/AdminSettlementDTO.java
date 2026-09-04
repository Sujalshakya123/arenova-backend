package com.arenova.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminSettlementDTO {

    private Long id;
    private Long eventId;
    private Long organizerId;
    private String tournament;
    private String gameName;
    private String organizerName;
    private String organizerEmail;
    private int paidEntryCount;
    private int registeredPlayerCount;
    private String entryFee;
    private String totalRevenue;
    private String platformShare;
    private String organizerShare;
    private String prizePool;
    private String firstPlacePrize;
    private String secondPlacePrize;
    private String firstPlaceWinner;
    private String secondPlaceWinner;
    private String status;
    private String initiatedAt;
    private String completedAt;
    private String settlementDate;
    private String failureReason;
    private boolean canApprove;
}

package com.arenova.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementDTO {

    private Long id;
    private Long eventId;
    private String eventTitle;
    private String gameName;
    private String status;

    private long totalRevenueNpr;
    private int paidEntryCount;
    private String entryFeeDisplay;

    private long platformAmountNpr;
    private long organizerAmountNpr;
    private long prizePoolAmountNpr;
    private long firstPlaceAmountNpr;
    private long secondPlaceAmountNpr;

    private String totalRevenueDisplay;
    private String platformAmountDisplay;
    private String organizerAmountDisplay;
    private String prizePoolAmountDisplay;
    private String firstPlaceAmountDisplay;
    private String secondPlaceAmountDisplay;

    private String firstPlaceWinnerName;
    private String secondPlaceWinnerName;
    private Long firstPlaceRegistrationId;
    private Long secondPlaceRegistrationId;

    private String initiatedAt;
    private String completedAt;
    private String failureReason;

    private String approvedAt;

    /** False when arenova.settlement.enabled=false (feature off). */
    private boolean settlementEnabled;

    private boolean canInitiate;
}

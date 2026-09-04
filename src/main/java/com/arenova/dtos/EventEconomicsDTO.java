package com.arenova.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventEconomicsDTO {

    /** "fixed" | "entry_fee_funded" */
    private String prizeFundingMode;

    /** Sum of completed entry-fee payments (NPR). */
    private long collectedTotalNpr;

    private int paidEntryCount;

    /** 70% slice for winners (NPR). */
    private long prizePoolCurrentNpr;

    /** 70% slice if all slots pay (NPR). */
    private long prizePoolAtCapacityNpr;

    private long organizerShareNpr;
    private long platformShareNpr;

    private long prizeFirstNpr;
    private long prizeSecondNpr;

    private String prizePoolDisplay;
    private String prizeFirstDisplay;
    private String prizeSecondDisplay;
    private String prizePoolAtCapacityDisplay;
}

package com.arenova.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizerReportRowDTO {

    private Long eventId;
    private String tournament;
    private String date;
    private String collectedAmount;
    private String commission;
    private String prize;
    private String sales;
    private String settlementStatus;
}

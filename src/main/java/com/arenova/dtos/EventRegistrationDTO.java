package com.arenova.dtos;

import com.arenova.dtos.enums.EventStatus;
import com.arenova.dtos.enums.Mode;
import com.arenova.dtos.enums.RegistrationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRegistrationDTO {

    private Long id;
    private Long eventId;
    private Long userId;
    private String teamName;
    private String teamTag;
    private String captainUsername;
    private List<String> roster;
    private String paymentMethod;
    private String paymentStatus;
    private RegistrationStatus status;
    private LocalDateTime registeredAt;

    /** Event snapshot for My Tournaments */
    private String eventTitle;
    private String gameName;
    private String imageKey;
    private String coverImageUrl;
    private String startDate;
    private String startTime;
    private EventStatus eventStatus;
    private Mode mode;
    private String prizePool;
    private String entry;
    private String maxCapacity;
    private Integer registeredCount;
    /** True when event is completed and this registration won the bracket. */
    private Boolean tournamentWinner;
    /** Prize pool amount if tournamentWinner, otherwise null. */
    private String prizeEarned;
}

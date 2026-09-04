package com.arenova.dtos;

import com.arenova.dtos.enums.EventStatus;
import com.arenova.dtos.enums.Mode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDTO {

    private Long id;
    private String title;
    private String minCapacity;
    private String maxCapacity;
    private LocalDateTime createdAt;
    private Mode mode;
    private String prizePool;
    private String prizeFirst;
    private String prizeSecond;
    private String prizeThird;
    private String entry;
    private String description;

    private Long projectId;
    private String gameName;
    private String imageKey;
    private String coverImageUrl;
    private String detailBannerUrl;
    private String detailBannerKey;
    private String platforms;
    private String startDate;
    private String startTime;
    private String timezone;
    private EventStatus status;
    private String participantType;
    private Integer registeredCount;
    private String matchType;
    private String stageType;
    private String bracketJson;
    private String bracketGeneratedAt;
    private String registrationDeadline;
    private Boolean registrationOpen;

    /** JSON for public detail extras (rules, schedule, badges, hostedBy). */
    private String publicPageJson;

    /** Populated when project organizer is loaded. */
    private String organizerName;
    private String organizerEmail;
    /** Organizer profile photo URL when set; null if none. */
    private String organizerPhotoUrl;

    /** "fixed" | "entry_fee_funded" — see arenova.prize-pool.mode */
    private String prizeFundingMode;

    private Long collectedTotalNpr;
    private Integer paidEntryCount;
    private Long prizePoolCurrentNpr;
    private Long prizePoolAtCapacityNpr;
    private Long organizerShareNpr;
    private Long platformShareNpr;

    private EventEconomicsDTO economics;
}

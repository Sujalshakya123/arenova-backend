package com.arenova.dtos;

import com.arenova.dtos.enums.EventStatus;
import com.arenova.dtos.enums.Mode;
import lombok.Data;

@Data
public class CreateEventRequest {
    private String title;
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
    private Mode mode;
    private String maxCapacity;
    private String minCapacity;
    private String prizePool;
    private String prizeFirst;
    private String prizeSecond;
    private String prizeThird;
    private String entry;
    private String description;
    private String participantType;
    private EventStatus status;
    private String matchType;
    private String stageType;
    private String bracketJson;
    private String bracketGeneratedAt;
    private String registrationDeadline;
    private Boolean registrationOpen;
    private String publicPageJson;
}

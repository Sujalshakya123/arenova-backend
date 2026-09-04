package com.arenova.mapper;

import com.arenova.dtos.EventDTO;
import com.arenova.entities.Event;
import com.arenova.entities.User;

public class EventMapper {

    public static EventDTO toDTO(Event event) {
        return EventDTO.builder()
                .id(event.getId())
                .title(event.getTitle())
                .minCapacity(event.getMinCapacity())
                .maxCapacity(event.getMaxCapacity())
                .createdAt(event.getCreatedAt())
                .mode(event.getMode())
                .prizePool(event.getPrizePool())
                .prizeFirst(event.getPrizeFirst())
                .prizeSecond(event.getPrizeSecond())
                .prizeThird(event.getPrizeThird())
                .entry(event.getEntry())
                .description(event.getDescription())
                .projectId(event.getProject() != null ? event.getProject().getId() : null)
                .gameName(event.getGameName())
                .imageKey(event.getImageKey())
                .coverImageUrl(event.getCoverImageUrl())
                .detailBannerUrl(event.getDetailBannerUrl())
                .detailBannerKey(event.getDetailBannerKey())
                .platforms(event.getPlatforms())
                .startDate(event.getStartDate())
                .startTime(event.getStartTime())
                .timezone(event.getTimezone())
                .status(event.getStatus())
                .participantType(event.getParticipantType())
                .registeredCount(
                        event.getRegisteredCount() != null ? event.getRegisteredCount() : 0
                )
                .matchType(event.getMatchType())
                .stageType(event.getStageType())
                .bracketJson(event.getBracketJson())
                .bracketGeneratedAt(event.getBracketGeneratedAt())
                .registrationDeadline(event.getRegistrationDeadline())
                .registrationOpen(
                        event.getRegistrationOpen() != null ? event.getRegistrationOpen() : true
                )
                .publicPageJson(event.getPublicPageJson())
                .organizerName(resolveOrganizerName(event))
                .organizerEmail(resolveOrganizerEmail(event))
                .organizerPhotoUrl(resolveOrganizerPhotoUrl(event))
                .build();
    }

    private static String resolveOrganizerName(Event event) {
        if (event.getProject() == null || event.getProject().getOrganizer() == null) {
            return null;
        }
        User organizer = event.getProject().getOrganizer();
        if (organizer.getFullName() != null && !organizer.getFullName().isBlank()) {
            return organizer.getFullName().trim();
        }
        if (organizer.getUsername() != null && !organizer.getUsername().isBlank()) {
            return organizer.getUsername().trim();
        }
        return organizer.getEmail();
    }

    private static String resolveOrganizerEmail(Event event) {
        if (event.getProject() == null || event.getProject().getOrganizer() == null) {
            return null;
        }
        return event.getProject().getOrganizer().getEmail();
    }

    private static String resolveOrganizerPhotoUrl(Event event) {
        if (event.getProject() == null || event.getProject().getOrganizer() == null) {
            return null;
        }
        String photoUrl = event.getProject().getOrganizer().getProfilePhotoUrl();
        if (photoUrl == null || photoUrl.isBlank()) {
            return null;
        }
        return photoUrl.trim();
    }
}

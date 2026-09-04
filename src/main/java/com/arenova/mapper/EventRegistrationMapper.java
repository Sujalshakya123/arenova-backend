package com.arenova.mapper;

import com.arenova.dtos.EventRegistrationDTO;
import com.arenova.dtos.enums.EventStatus;
import com.arenova.entities.Event;
import com.arenova.entities.EventRegistration;
import com.arenova.util.BracketCompletionUtil;

import java.util.Collections;
import java.util.List;

public class EventRegistrationMapper {

    public static List<String> rosterOrEmpty(List<String> roster) {
        return roster != null ? roster : Collections.emptyList();
    }

    public static EventRegistrationDTO toDTO(EventRegistration registration) {
        Event event = registration.getEvent();
        String champion = event != null && event.getStatus() == EventStatus.COMPLETED
                ? BracketCompletionUtil.findChampion(event.getBracketJson())
                : null;
        String runnerUp = event != null && event.getStatus() == EventStatus.COMPLETED
                ? BracketCompletionUtil.findRunnerUp(event.getBracketJson())
                : null;
        boolean isWinner = BracketCompletionUtil.registrationIsChampion(
                registration,
                event,
                champion
        );
        boolean isSecond = BracketCompletionUtil.registrationIsRunnerUp(
                registration,
                event,
                runnerUp
        );

        String prizeEarned = null;
        if (event != null) {
            if (isWinner) {
                prizeEarned = firstNonBlank(event.getPrizeFirst(), event.getPrizePool());
            } else if (isSecond) {
                prizeEarned = firstNonBlank(event.getPrizeSecond(), null);
            }
        }

        return EventRegistrationDTO.builder()
                .id(registration.getId())
                .eventId(event != null ? event.getId() : null)
                .userId(registration.getUser() != null ? registration.getUser().getId() : null)
                .teamName(registration.getTeamName())
                .teamTag(registration.getTeamTag())
                .captainUsername(registration.getCaptainUsername())
                .roster(rosterOrEmpty(registration.getRoster()))
                .paymentMethod(registration.getPaymentMethod())
                .status(registration.getStatus())
                .registeredAt(registration.getRegisteredAt())
                .eventTitle(event != null ? event.getTitle() : null)
                .gameName(event != null ? event.getGameName() : null)
                .imageKey(event != null ? event.getImageKey() : null)
                .coverImageUrl(event != null ? event.getCoverImageUrl() : null)
                .startDate(event != null ? event.getStartDate() : null)
                .startTime(event != null ? event.getStartTime() : null)
                .eventStatus(event != null ? event.getStatus() : null)
                .mode(event != null ? event.getMode() : null)
                .prizePool(event != null ? event.getPrizePool() : null)
                .entry(event != null ? event.getEntry() : null)
                .maxCapacity(event != null ? event.getMaxCapacity() : null)
                .registeredCount(
                        event != null && event.getRegisteredCount() != null
                                ? event.getRegisteredCount()
                                : 0
                )
                .tournamentWinner(isWinner)
                .prizeEarned(prizeEarned)
                .build();
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback.trim();
        }
        return null;
    }
}

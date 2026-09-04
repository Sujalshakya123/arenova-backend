package com.arenova.services;

import com.arenova.dtos.EventDTO;
import com.arenova.dtos.EventEconomicsDTO;
import com.arenova.dtos.EventRegistrationDTO;
import com.arenova.dtos.enums.PaymentStatus;
import com.arenova.entities.Event;
import com.arenova.entities.EventRegistration;
import com.arenova.entities.Payment;
import com.arenova.respositories.PaymentRepository;
import com.arenova.util.BracketCompletionUtil;
import com.arenova.util.EntryFeeUtil;
import com.arenova.config.PrizePoolProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PrizePoolService {

    private final PrizePoolProperties properties;
    private final PaymentRepository paymentRepository;

    public EventEconomicsDTO calculate(Event event) {
        if (event == null || !properties.isEntryFeeFunded()) {
            return null;
        }

        long collected = sumCompletedPayments(event.getId());
        int paidCount = countCompletedPayments(event.getId());
        int entryFee = EntryFeeUtil.parseEntryFeeNpr(event.getEntry());
        int maxSlots = parseMaxCapacity(event.getMaxCapacity());
        long atCapacityCollected = (long) entryFee * maxSlots;

        long prizeSlice = percentOf(collected, properties.getPrizePercent());
        long organizerSlice = percentOf(collected, properties.getOrganizerPercent());
        long platformSlice = percentOf(collected, properties.getPlatformPercent());
        long atCapacityPrize = percentOf(atCapacityCollected, properties.getPrizePercent());

        // 1st/2nd are % of total revenue (e.g. 40% / 30%), not % of the 70% prize slice.
        long firstPlace = percentOf(collected, properties.getFirstPlacePercent());
        long secondPlace = percentOf(collected, properties.getSecondPlacePercent());

        return EventEconomicsDTO.builder()
                .prizeFundingMode("entry_fee_funded")
                .collectedTotalNpr(collected)
                .paidEntryCount(paidCount)
                .prizePoolCurrentNpr(prizeSlice)
                .prizePoolAtCapacityNpr(atCapacityPrize)
                .organizerShareNpr(organizerSlice)
                .platformShareNpr(platformSlice)
                .prizeFirstNpr(firstPlace)
                .prizeSecondNpr(secondPlace)
                .prizePoolDisplay(formatRs(prizeSlice))
                .prizeFirstDisplay(formatRs(firstPlace))
                .prizeSecondDisplay(formatRs(secondPlace))
                .prizePoolAtCapacityDisplay(formatRs(atCapacityPrize))
                .build();
    }

    public boolean isEntryFeeFunded() {
        return properties.isEntryFeeFunded();
    }

    public void enrichEventDto(EventDTO dto, Event event) {
        if (dto == null || event == null) {
            return;
        }
        if (!properties.isEntryFeeFunded()) {
            dto.setPrizeFundingMode("fixed");
            return;
        }

        EventEconomicsDTO economics = calculate(event);
        if (economics == null) {
            return;
        }

        dto.setPrizeFundingMode(economics.getPrizeFundingMode());
        dto.setCollectedTotalNpr(economics.getCollectedTotalNpr());
        dto.setPaidEntryCount(economics.getPaidEntryCount());
        dto.setPrizePoolCurrentNpr(economics.getPrizePoolCurrentNpr());
        dto.setPrizePoolAtCapacityNpr(economics.getPrizePoolAtCapacityNpr());
        dto.setOrganizerShareNpr(economics.getOrganizerShareNpr());
        dto.setPlatformShareNpr(economics.getPlatformShareNpr());
        dto.setPrizePool(economics.getPrizePoolDisplay());
        dto.setPrizeFirst(economics.getPrizeFirstDisplay());
        dto.setPrizeSecond(economics.getPrizeSecondDisplay());
        dto.setPrizeThird(null);
        dto.setEconomics(economics);
    }

    public void enrichRegistrationDto(EventRegistrationDTO dto, EventRegistration registration) {
        if (dto == null || registration == null || !properties.isEntryFeeFunded()) {
            return;
        }

        Event event = registration.getEvent();
        if (event == null) {
            return;
        }

        EventEconomicsDTO economics = calculate(event);
        if (economics == null) {
            return;
        }

        dto.setPrizePool(economics.getPrizePoolDisplay());

        if (event.getStatus() != com.arenova.dtos.enums.EventStatus.COMPLETED) {
            return;
        }

        String champion = BracketCompletionUtil.findChampion(event.getBracketJson());
        String runnerUp = BracketCompletionUtil.findRunnerUp(event.getBracketJson());

        if (BracketCompletionUtil.registrationIsChampion(registration, event, champion)) {
            dto.setTournamentWinner(true);
            dto.setPrizeEarned(economics.getPrizeFirstDisplay());
        } else if (BracketCompletionUtil.registrationIsRunnerUp(registration, event, runnerUp)) {
            dto.setTournamentWinner(false);
            dto.setPrizeEarned(economics.getPrizeSecondDisplay());
        }
    }

    private long sumCompletedPayments(Long eventId) {
        if (eventId == null) {
            return 0;
        }
        return paymentRepository.findByEvent_IdAndStatus(eventId, PaymentStatus.COMPLETED).stream()
                .mapToLong(this::parsePaymentAmount)
                .sum();
    }

    private int countCompletedPayments(Long eventId) {
        if (eventId == null) {
            return 0;
        }
        return paymentRepository.findByEvent_IdAndStatus(eventId, PaymentStatus.COMPLETED).size();
    }

    private long parsePaymentAmount(Payment payment) {
        if (payment == null) {
            return 0;
        }
        return EntryFeeUtil.parseNprAmount(payment.getAmount());
    }

    private int parseMaxCapacity(String maxCapacity) {
        if (maxCapacity == null || maxCapacity.isBlank()) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(maxCapacity.replaceAll("[^0-9]", "")));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private long percentOf(long amount, int percent) {
        if (amount <= 0 || percent <= 0) {
            return 0;
        }
        return (amount * percent) / 100;
    }

    public static String formatRs(long npr) {
        if (npr <= 0) {
            return "Rs. 0";
        }
        return String.format("Rs. %,d", npr);
    }
}

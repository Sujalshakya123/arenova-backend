package com.arenova.entities;

import com.arenova.dtos.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "event_settlements",
        uniqueConstraints = @UniqueConstraint(name = "uk_settlement_event", columnNames = "event_id")
)
public class EventSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, unique = true)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

    private long totalRevenueNpr;

    private int paidEntryCount;

    @Column(length = 64)
    private String entryFeeSnapshot;

    private long platformAmountNpr;
    private long organizerAmountNpr;
    private long prizePoolAmountNpr;
    private long firstPlaceAmountNpr;
    private long secondPlaceAmountNpr;

    private Long firstPlaceRegistrationId;
    private Long secondPlaceRegistrationId;

    @Column(length = 200)
    private String firstPlaceWinnerName;

    @Column(length = 200)
    private String secondPlaceWinnerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    @Builder.Default
    private SettlementStatus status = SettlementStatus.PROCESSING;

    private LocalDateTime initiatedAt;
    private LocalDateTime completedAt;

    @Column(length = 1000)
    private String failureReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private User approvedBy;

    private LocalDateTime approvedAt;

    @PrePersist
    protected void onCreate() {
        if (initiatedAt == null) {
            initiatedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = SettlementStatus.PROCESSING;
        }
    }
}

package com.arenova.entities;

import com.arenova.dtos.enums.EventStatus;
import com.arenova.dtos.enums.Mode;
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
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String minCapacity;

    private String maxCapacity;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = EventStatus.DRAFT;
        }
        if (this.registeredCount == null) {
            this.registeredCount = 0;
        }
    }

    @Enumerated(EnumType.STRING)
    private Mode mode;

    private String prizePool;

    /** Optional place amounts, same display format as prizePool (e.g. "Rs. 25,000"). */
    private String prizeFirst;
    private String prizeSecond;
    private String prizeThird;

    private String entry;

    @Column(length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    /** Display name e.g. "Free Fire", "PUBG Mobile" */
    private String gameName;

    /** Maps to frontend GAME_BANNERS / GAME_COVERS key (freefire, pubg, ...) */
    private String imageKey;

    /**
     * Optional override for public card cover.
     * Absolute upload URL or leave null to resolve from imageKey on the client.
     */
    private String coverImageUrl;

    /**
     * Optional wide hero for tournament detail page (separate from card cover).
     * Absolute upload URL, or leave null to use detailBannerKey / card cover.
     */
    private String detailBannerUrl;

    /** Optional key for detail hero (GAME_BANNERS), independent of card imageKey */
    private String detailBannerKey;

    /** Comma-separated platforms e.g. "PC,Mobile" */
    private String platforms;

    private String startDate;

    private String startTime;

    private String timezone;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EventStatus status = EventStatus.DRAFT;

    /** "players" or "team" */
    private String participantType;

    @Builder.Default
    private Integer registeredCount = 0;

    /** ISO date string e.g. 2026-08-25 — registration closes after this day */
    private String registrationDeadline;

    @Builder.Default
    private Boolean registrationOpen = true;

    /** "duel" | "ffa" */
    private String matchType;

    /** e.g. single-elimination, double-elimination */
    private String stageType;

    /** JSON array of bracket matches */
    @Column(columnDefinition = "TEXT")
    private String bracketJson;

    private String bracketGeneratedAt;

    /**
     * JSON for public detail extras (rules, schedule stages, badges, hostedBy, etc.).
     * Description and registrationDeadline stay in their own columns.
     */
    @Column(columnDefinition = "TEXT")
    private String publicPageJson;
}

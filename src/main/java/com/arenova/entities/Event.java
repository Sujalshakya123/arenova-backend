package com.arenova.entities;


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

    @PrePersist  // ✅ auto set on save
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Enumerated(EnumType.STRING)
    private Mode mode;

    private String prizePool;

    private String entry;

    private String description;



}

package com.arenova.entities;

import com.arenova.dtos.enums.GameStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Entity
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Frontend-friendly id, e.g. valorant, pubg-mobile */
    @Column(unique = true)
    private String slug;

    private String gname;              // "Valorant"

    private String genre;             // "Tactical Shooter"

    @Column(length = 2000)
    private String description;       // short pitch

    @Column(length = 4000)
    private String about;             // longer about text

    private String partner;           // "Official League Partner"

    private String developer;         // "Riot Games"

    private String releaseDate;       // "June 2, 2020"

    private String platforms;         // "Windows"

    @Column(length = 2000)
    private String bannerImageUrl;    // hero banner image

    @Column(length = 2000)
    private String iconImageUrl;      // cover / icon

    private String imageKey;          // valorant, pubg, freefire, ...

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private GameStatus status = GameStatus.AVAILABLE;

}

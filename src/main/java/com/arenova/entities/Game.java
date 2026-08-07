package com.arenova.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

    private String gname;              // "Valorant"

    private String genre;             // "Tactical Shooter"

    private String description;       // "A 5v5 character-based tactical FPS..."

    private String developer;         // "Riot Games"

    private String releaseDate;       // "June 2, 2020"

    private String platforms;         // "Windows"

    private String bannerImageUrl;    // hero banner image

    private String iconImageUrl;      // small game icon (bottom right)


}

package com.arenova.dtos.enums;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameDTO {

    private Long id;
    private String slug;
    private String gname;
    private String genre;
    private String description;
    private String about;
    private String partner;
    private String developer;
    private String releaseDate;
    private String platforms;
    private String bannerImageUrl;
    private String iconImageUrl;
    private String imageKey;
    private GameStatus status;

}

package com.arenova.mapper;

import com.arenova.dtos.enums.GameDTO;
import com.arenova.entities.Game;

public class GameMapper {

    public static GameDTO toDTO (Game game){
        return new GameDTO(
                game.getId(),
                game.getGname(),
                game.getGenre(),
                game.getDescription(),
                game.getDeveloper(),
                game.getReleaseDate(),
                game.getPlatforms(),
                game.getBannerImageUrl(),
                game.getIconImageUrl()
        );
    }

    //Event DTO - Event Entity Convert

    public static Game toEntity(GameDTO gameDTO){
        return new Game(
                gameDTO.getId(),
                gameDTO.getGname(),
                gameDTO.getGenre(),
                gameDTO.getDescription(),
                gameDTO.getDeveloper(),
                gameDTO.getReleaseDate(),
                gameDTO.getPlatforms(),
                gameDTO.getBannerImageUrl(),
                gameDTO.getIconImageUrl()
        );
    }
}

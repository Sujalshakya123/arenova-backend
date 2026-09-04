package com.arenova.mapper;

import com.arenova.dtos.enums.GameDTO;
import com.arenova.dtos.enums.GameStatus;
import com.arenova.entities.Game;

public class GameMapper {

    public static GameDTO toDTO (Game game){
        return new GameDTO(
                game.getId(),
                game.getSlug(),
                game.getGname(),
                game.getGenre(),
                game.getDescription(),
                game.getAbout(),
                game.getPartner(),
                game.getDeveloper(),
                game.getReleaseDate(),
                game.getPlatforms(),
                game.getBannerImageUrl(),
                game.getIconImageUrl(),
                game.getImageKey(),
                game.getStatus() != null ? game.getStatus() : GameStatus.AVAILABLE
        );
    }

    public static Game toEntity(GameDTO gameDTO){
        return new Game(
                gameDTO.getId(),
                gameDTO.getSlug(),
                gameDTO.getGname(),
                gameDTO.getGenre(),
                gameDTO.getDescription(),
                gameDTO.getAbout(),
                gameDTO.getPartner(),
                gameDTO.getDeveloper(),
                gameDTO.getReleaseDate(),
                gameDTO.getPlatforms(),
                gameDTO.getBannerImageUrl(),
                gameDTO.getIconImageUrl(),
                gameDTO.getImageKey(),
                gameDTO.getStatus() != null ? gameDTO.getStatus() : GameStatus.AVAILABLE
        );
    }
}

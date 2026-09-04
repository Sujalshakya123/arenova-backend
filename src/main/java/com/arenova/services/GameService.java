package com.arenova.services;

import com.arenova.dtos.enums.GameDTO;

import java.util.List;

public interface GameService {

    //Create Game
    GameDTO createGame(GameDTO gameDTO);

    //Update Game
    GameDTO updateGame(Long id, GameDTO gameDTO);

    //Delete Game
    void deleteGame(Long id);

    //Get all games
    List<GameDTO> getAllGames();

    //Get game by id
    GameDTO getGameById(Long id);

    //Get game by slug (frontend id)
    GameDTO getGameBySlug(String slug);

    GameDTO uploadBanner(Long id, org.springframework.web.multipart.MultipartFile file)
            throws Exception;

    GameDTO uploadIcon(Long id, org.springframework.web.multipart.MultipartFile file)
            throws Exception;

}

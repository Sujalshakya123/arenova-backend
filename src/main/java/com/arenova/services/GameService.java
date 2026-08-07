package com.arenova.services;

import com.arenova.dtos.enums.GameDTO;
import org.springframework.stereotype.Service;

@Service
public interface GameService {

    //Create Game
    GameDTO createGame(GameDTO gameDTO);

    //Update Game
    GameDTO updateGame(Long id, GameDTO eventDTO);

    //Delete Event
    void deleteGame(Long id);


    //Get game by id
    GameDTO getGameById(Long id);

}

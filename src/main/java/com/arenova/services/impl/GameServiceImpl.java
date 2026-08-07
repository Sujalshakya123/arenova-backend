package com.arenova.services.impl;

import com.arenova.dtos.enums.GameDTO;
import com.arenova.entities.Game;
import com.arenova.entities.User;
import com.arenova.exceptions.ResourceNotFoundException;
import com.arenova.mapper.GameMapper;
import com.arenova.mapper.UserMapper;
import com.arenova.respositories.GameRepository;
import com.arenova.services.GameService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@AllArgsConstructor
public class GameServiceImpl implements GameService {


    private final GameRepository gameRepository;


    @Override
    public GameDTO createGame(GameDTO gameDTO) {
        Game game = GameMapper.toEntity(gameDTO);
        Game savedGame = gameRepository.save(game);
        return GameMapper.toDTO(savedGame);
    }

    @Override
    public GameDTO getGameById(Long id) {
        Game game = gameRepository.findById(id).
                orElseThrow(()-> new ResourceNotFoundException("Game ID Not Found")); //Entity value
        return GameMapper.toDTO(game);
    }

    @Override
    public GameDTO updateGame(Long id, GameDTO gameDTO) {
        Game game = gameRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("Game ID NOT FOUND"));

        game.setGname(gameDTO.getGname());
        game.setGenre(gameDTO.getGenre());
        game.setDescription(gameDTO.getDescription());
        game.setDeveloper(gameDTO.getDeveloper());
        game.setReleaseDate(gameDTO.getReleaseDate());
        game.setPlatforms(gameDTO.getPlatforms());

        Game updatedGame = gameRepository.save(game);
        return GameMapper.toDTO(updatedGame);
    }

    @Override
    public void deleteGame(Long id) {
        Game game = gameRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("Game ID NOT FOUND"));
        gameRepository.delete(game);
    }


}

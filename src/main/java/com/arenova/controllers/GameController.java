package com.arenova.controllers;


import com.arenova.dtos.enums.GameDTO;
import com.arenova.services.GameService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "http://localhost:5173")
@AllArgsConstructor
public class GameController {

    private final GameService gameService;

    @PostMapping
    public ResponseEntity<GameDTO> createGame(@RequestBody GameDTO gameDTO){
        GameDTO savedGame = gameService.createGame(gameDTO);
        return new ResponseEntity<>(savedGame, HttpStatus.CREATED);
    }

    @GetMapping("{id}")
    public ResponseEntity<GameDTO>  getGameById(@PathVariable() Long id){
        GameDTO game = gameService.getGameById(id);
        return new ResponseEntity<>(game, HttpStatus.FOUND);
    }

    @PutMapping("{id}")
    public ResponseEntity<GameDTO> updateGame(@PathVariable Long id, @RequestBody GameDTO gameDTO){
        GameDTO game= gameService.updateGame(id, gameDTO);
        return new ResponseEntity<>(game , HttpStatus.OK);
    }
}

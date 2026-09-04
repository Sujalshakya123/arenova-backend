package com.arenova.controllers;


import com.arenova.dtos.enums.GameDTO;
import com.arenova.services.GameService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

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

    @GetMapping
    public ResponseEntity<List<GameDTO>> getAllGames() {
        return ResponseEntity.ok(gameService.getAllGames());
    }

    @GetMapping("{id}")
    public ResponseEntity<GameDTO> getGameById(@PathVariable String id){
        // Support numeric DB id or frontend slug (e.g. valorant)
        if (id.matches("\\d+")) {
            return ResponseEntity.ok(gameService.getGameById(Long.parseLong(id)));
        }
        return ResponseEntity.ok(gameService.getGameBySlug(id));
    }

    @PutMapping("{id}")
    public ResponseEntity<GameDTO> updateGame(@PathVariable Long id, @RequestBody GameDTO gameDTO){
        GameDTO game= gameService.updateGame(id, gameDTO);
        return new ResponseEntity<>(game , HttpStatus.OK);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteGame(@PathVariable Long id) {
        gameService.deleteGame(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/banner")
    public ResponseEntity<?> uploadBanner(
            @PathVariable Long id,
            @RequestParam("banner") MultipartFile file
    ) {
        try {
            GameDTO updated = gameService.uploadBanner(id, file);
            return ResponseEntity.ok(Map.of("game", updated));
        } catch (org.apache.coyote.BadRequestException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/icon")
    public ResponseEntity<?> uploadIcon(
            @PathVariable Long id,
            @RequestParam("icon") MultipartFile file
    ) {
        try {
            GameDTO updated = gameService.uploadIcon(id, file);
            return ResponseEntity.ok(Map.of("game", updated));
        } catch (org.apache.coyote.BadRequestException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }
}

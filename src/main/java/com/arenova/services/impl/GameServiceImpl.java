package com.arenova.services.impl;

import com.arenova.dtos.enums.GameDTO;
import com.arenova.dtos.enums.GameStatus;
import com.arenova.entities.Game;
import com.arenova.exceptions.ResourceNotFoundException;
import com.arenova.mapper.GameMapper;
import com.arenova.respositories.GameRepository;
import com.arenova.services.GameService;
import lombok.AllArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@AllArgsConstructor
public class GameServiceImpl implements GameService {


    private final GameRepository gameRepository;


    private String toSlug(String name) {
        if (name == null || name.isBlank()) {
            return "game-" + System.currentTimeMillis();
        }
        return name
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    @Override
    public GameDTO createGame(GameDTO gameDTO) {
        Game game = GameMapper.toEntity(gameDTO);

        if (game.getSlug() == null || game.getSlug().isBlank()) {
            game.setSlug(toSlug(game.getGname()));
        }
        if (game.getStatus() == null) {
            game.setStatus(GameStatus.AVAILABLE);
        }

        Game savedGame = gameRepository.save(game);
        return GameMapper.toDTO(savedGame);
    }

    @Override
    public GameDTO getGameById(Long id) {
        Game game = gameRepository.findById(id).
                orElseThrow(()-> new ResourceNotFoundException("Game ID Not Found"));
        return GameMapper.toDTO(game);
    }

    @Override
    public GameDTO getGameBySlug(String slug) {
        Game game = gameRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Game not found: " + slug));
        return GameMapper.toDTO(game);
    }

    @Override
    public List<GameDTO> getAllGames() {
        return gameRepository.findAll()
                .stream()
                .map(GameMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public GameDTO updateGame(Long id, GameDTO gameDTO) {
        Game game = gameRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("Game ID NOT FOUND"));

        if (gameDTO.getSlug() != null && !gameDTO.getSlug().isBlank()) {
            game.setSlug(gameDTO.getSlug());
        }
        if (gameDTO.getGname() != null) {
            game.setGname(gameDTO.getGname());
        }
        if (gameDTO.getGenre() != null) {
            game.setGenre(gameDTO.getGenre());
        }
        if (gameDTO.getDescription() != null) {
            game.setDescription(gameDTO.getDescription());
        }
        if (gameDTO.getAbout() != null) {
            game.setAbout(gameDTO.getAbout());
        }
        if (gameDTO.getPartner() != null) {
            game.setPartner(gameDTO.getPartner());
        }
        if (gameDTO.getDeveloper() != null) {
            game.setDeveloper(gameDTO.getDeveloper());
        }
        if (gameDTO.getReleaseDate() != null) {
            game.setReleaseDate(gameDTO.getReleaseDate());
        }
        if (gameDTO.getPlatforms() != null) {
            game.setPlatforms(gameDTO.getPlatforms());
        }
        if (gameDTO.getBannerImageUrl() != null) {
            game.setBannerImageUrl(gameDTO.getBannerImageUrl());
        }
        if (gameDTO.getIconImageUrl() != null) {
            game.setIconImageUrl(gameDTO.getIconImageUrl());
        }
        if (gameDTO.getImageKey() != null) {
            game.setImageKey(gameDTO.getImageKey());
        }
        if (gameDTO.getStatus() != null) {
            game.setStatus(gameDTO.getStatus());
        }

        Game updatedGame = gameRepository.save(game);
        return GameMapper.toDTO(updatedGame);
    }

    @Override
    public void deleteGame(Long id) {
        Game game = gameRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("Game ID NOT FOUND"));
        gameRepository.delete(game);
    }

    private String storeUploadedImage(MultipartFile file, String prefix) throws java.io.IOException {
        String uploadDir = "uploads/";
        Files.createDirectories(Paths.get(uploadDir));
        String original = file.getOriginalFilename();
        String ext = ".jpg";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.'));
        }
        String filename = prefix + "-" + UUID.randomUUID() + ext;
        Path filePath = Paths.get(uploadDir + filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return "http://localhost:8080/uploads/" + filename;
    }

    @Override
    public GameDTO uploadBanner(Long id, MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Banner file is required.");
        }
        Game game = gameRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Game ID NOT FOUND"));
        game.setBannerImageUrl(storeUploadedImage(file, "game-banner"));
        return GameMapper.toDTO(gameRepository.save(game));
    }

    @Override
    public GameDTO uploadIcon(Long id, MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Icon file is required.");
        }
        Game game = gameRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Game ID NOT FOUND"));
        game.setIconImageUrl(storeUploadedImage(file, "game-icon"));
        return GameMapper.toDTO(gameRepository.save(game));
    }


}

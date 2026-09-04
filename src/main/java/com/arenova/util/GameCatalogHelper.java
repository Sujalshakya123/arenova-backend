package com.arenova.util;

import com.arenova.dtos.enums.GameStatus;
import com.arenova.entities.Game;
import com.arenova.respositories.GameRepository;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GameCatalogHelper {

    private final GameRepository gameRepository;

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    public Optional<Game> findByName(String gameName) {
        if (gameName == null || gameName.isBlank()) {
            return Optional.empty();
        }
        String normalized = normalize(gameName);
        List<Game> games = gameRepository.findAll();
        for (Game game : games) {
            String gname = normalize(game.getGname());
            String slug = normalize(game.getSlug());
            if (normalized.equals(gname) || normalized.equals(slug)) {
                return Optional.of(game);
            }
            if (!gname.isEmpty() && (gname.contains(normalized) || normalized.contains(gname))) {
                return Optional.of(game);
            }
            if (!slug.isEmpty() && (slug.contains(normalized) || normalized.contains(slug))) {
                return Optional.of(game);
            }
        }
        return Optional.empty();
    }

    public boolean isAvailableForTournaments(String gameName) {
        return findByName(gameName)
                .map(game -> game.getStatus() == GameStatus.AVAILABLE)
                .orElse(true);
    }

    public void requireAvailableGame(String gameName) throws BadRequestException {
        if (gameName == null || gameName.isBlank()) {
            throw new BadRequestException("Game / discipline is required.");
        }
        Optional<Game> game = findByName(gameName);
        if (game.isEmpty()) {
            throw new BadRequestException("Selected game is not in the platform catalog.");
        }
        if (game.get().getStatus() != GameStatus.AVAILABLE) {
            throw new BadRequestException(
                    "Tournaments cannot be created for games marked Coming Soon. "
                            + "Ask a platform admin to activate the game first."
            );
        }
    }
}

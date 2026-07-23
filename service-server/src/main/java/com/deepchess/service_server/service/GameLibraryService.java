package com.deepchess.service_server.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.deepchess.service_server.entity.Game;
import com.deepchess.service_server.entity.User;
import com.deepchess.service_server.repository.GameRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GameLibraryService {

    private final GameRepository gameRepository;
    private final GameAccessService gameAccessService;

    @Transactional(readOnly = true)
    public List<Game> findSavedGames(User user, String keyword, SavedGameSort savedGameSort) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.isEmpty()) {
            return gameRepository.findByUserAndIsSavedTrue(user, savedGameSort.toSort());
        }
        return gameRepository.findByUserAndIsSavedTrueAndTitleContainingIgnoreCase(
                user,
                normalizedKeyword,
                savedGameSort.toSort());
    }

    @Transactional
    public Game saveOwnedGame(Long gameId, User user, String title) {
        Game game = gameAccessService.requireOwnedGame(gameId, user);
        try {
            game.markAsSaved(title);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
        return game;
    }

    @Transactional
    public Game renameSavedGame(Long gameId, User user, String title) {
        Game game = gameAccessService.requireOwnedSavedGame(gameId, user);
        try {
            game.rename(title);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
        return game;
    }
}

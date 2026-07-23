package com.deepchess.service_server.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.deepchess.service_server.entity.Game;
import com.deepchess.service_server.repository.GameRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GameTitleMigrationService {

    private final GameRepository gameRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void backfillSavedGameTitles() {
        for (Game game : gameRepository.findSavedGamesWithoutTitle()) {
            game.assignDefaultTitleIfMissing();
        }
    }
}

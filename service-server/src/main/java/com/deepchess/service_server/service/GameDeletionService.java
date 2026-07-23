package com.deepchess.service_server.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.deepchess.service_server.entity.User;
import com.deepchess.service_server.repository.AnalysisRepository;
import com.deepchess.service_server.repository.GameRepository;
import com.deepchess.service_server.repository.PositionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GameDeletionService {

    private final GameAccessService gameAccessService;
    private final AnalysisRepository analysisRepository;
    private final PositionRepository positionRepository;
    private final GameRepository gameRepository;

    @Transactional
    public void deleteOwnedSavedGame(Long gameId, User user) {
        gameAccessService.requireOwnedSavedGame(gameId, user);

        analysisRepository.deleteByPositionGameId(gameId);
        positionRepository.clearParentReferencesByGameId(gameId);
        positionRepository.deleteByGameId(gameId);
        gameRepository.deleteByGameId(gameId);
    }
}

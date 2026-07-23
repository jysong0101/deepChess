package com.deepchess.service_server.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.deepchess.service_server.entity.Game;
import com.deepchess.service_server.entity.Position;
import com.deepchess.service_server.entity.User;
import com.deepchess.service_server.repository.GameRepository;
import com.deepchess.service_server.repository.PositionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GameAccessService {

    private final GameRepository gameRepository;
    private final PositionRepository positionRepository;

    public Game requireOwnedGame(Long gameId, User user) {
        return gameRepository.findByGameIdAndUser(gameId, user)
                .orElseThrow(this::notFound);
    }

    public Game requireOwnedSavedGame(Long gameId, User user) {
        return gameRepository.findByGameIdAndUserAndIsSavedTrue(gameId, user)
                .orElseThrow(this::notFound);
    }

    public Position requirePositionInGame(Long positionId, Long gameId, User user) {
        return positionRepository
                .findByPositionIdAndGame_GameIdAndGame_User(positionId, gameId, user)
                .orElseThrow(this::notFound);
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "게임을 찾을 수 없습니다.");
    }
}

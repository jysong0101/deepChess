package com.deepchess.service_server.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.deepchess.service_server.entity.Analysis;
import com.deepchess.service_server.dto.response.AnalysisResponse;
import com.deepchess.service_server.entity.Game;
import com.deepchess.service_server.entity.Position;
import com.deepchess.service_server.entity.User;
import com.deepchess.service_server.repository.AnalysisRepository;
import com.deepchess.service_server.repository.PositionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnalysisRecordService {

    private final PositionRepository positionRepository;
    private final AnalysisRepository analysisRepository;
    private final ChessEngineService chessEngineService;
    private final GameAccessService gameAccessService;

    @Transactional // 💡 파라미터에 positionId 가 추가되었습니다.
    public AnalysisResponse analyzeAndSave(
            User user,
            Long gameId,
            Long positionId,
            Long parentPositionId,
            String fen,
            String moveSan,
            int depth) {
        Game game = gameAccessService.requireOwnedGame(gameId, user);
        Position requestedPosition = positionId == null
                ? null
                : gameAccessService.requirePositionInGame(positionId, gameId, user);
        Position requestedParent = parentPositionId == null
                ? null
                : gameAccessService.requirePositionInGame(parentPositionId, gameId, user);

        Position position;
        if (requestedPosition != null) {
            position = requestedPosition;
            if (!position.getFen().equals(fen)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Position의 FEN이 일치하지 않습니다.");
            }
        } else {
            AnalysisResponse result = chessEngineService.analyzePosition(fen, depth);
            position = Position.builder()
                    .game(game)
                    .fen(fen)
                    .parentPosition(requestedParent)
                    .moveSan(moveSan)
                    .build();
            position = positionRepository.save(position);

            saveAnalysisIfAbsent(position, result, depth);
            return result.withPositionId(position.getPositionId());
        }

        AnalysisResponse result = chessEngineService.analyzePosition(fen, depth);
        saveAnalysisIfAbsent(position, result, depth);
        return result.withPositionId(position.getPositionId());
    }

    private void saveAnalysisIfAbsent(Position position, AnalysisResponse result, int depth) {
        Analysis analysis = analysisRepository.findByPosition_PositionId(position.getPositionId()).orElse(null);
        if (analysis == null) {
            analysis = Analysis.builder()
                    .position(position)
                    .engineScore(result.engineScore())
                    .bestMoveUci(result.bestMoveUci())
                    .depth(depth)
                    .analysisDetail(result.analysisDetail())
                    .build();
            analysisRepository.save(analysis);
        }
    }
}

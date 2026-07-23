package com.deepchess.service_server.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.deepchess.service_server.entity.Analysis;
import com.deepchess.service_server.dto.response.AnalysisResponse;
import com.deepchess.service_server.entity.Game;
import com.deepchess.service_server.entity.Position;
import com.deepchess.service_server.repository.AnalysisRepository;
import com.deepchess.service_server.repository.GameRepository;
import com.deepchess.service_server.repository.PositionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnalysisRecordService {

    private final GameRepository gameRepository;
    private final PositionRepository positionRepository;
    private final AnalysisRepository analysisRepository;
    private final ChessEngineService chessEngineService;

    @Transactional // 💡 파라미터에 positionId 가 추가되었습니다.
    public AnalysisResponse analyzeAndSave(
            Long gameId,
            Long positionId,
            Long parentPositionId,
            String fen,
            String moveSan,
            int depth) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("게임을 찾을 수 없습니다."));

        AnalysisResponse result = chessEngineService.analyzePosition(fen, depth);

        Position position;
        // 💡 1. 기보 불러오기로 이미 DB에 존재하는 수라면? -> 복제하지 않고 찾아온다!
        if (positionId != null) {
            position = positionRepository.findById(positionId).orElseThrow();
        } 
        // 💡 2. 유저가 보드판에서 새로 둔 수라면? -> 새로 생성한다!
        else {
            Position parentPosition = null;
            if (parentPositionId != null) {
                parentPosition = positionRepository.findById(parentPositionId).orElse(null);
            }
            position = Position.builder()
                    .game(game)
                    .fen(fen)
                    .parentPosition(parentPosition)
                    .moveSan(moveSan)
                    .build();
            position = positionRepository.save(position);
        }

        // 💡 3. 분석(Analysis) 데이터도 중복 생성을 막기 위해 확인 후 저장
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

        return result.withPositionId(position.getPositionId());
    }
}

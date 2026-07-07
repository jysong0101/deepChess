package com.deepchess.service_server.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.deepchess.service_server.entity.Analysis;
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

    @Transactional
    public Map<String, Object> analyzeAndSave(Long gameId, Long parentPositionId, String fen, String moveSan, int depth) {        
        // 1. 기존에 만들어둔 게임(Game) 찾기
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("게임을 찾을 수 없습니다."));

        // 2. 엔진 분석 수행
        Map<String, Object> result = chessEngineService.analyzePosition(fen, depth);

        // 3. 부모 국면 찾기 (첫 수라면 null일 수 있음)
        Position parentPosition = null;
        if (parentPositionId != null) {
            parentPosition = positionRepository.findById(parentPositionId).orElse(null);
        }

        // 4. 새 국면(Position) 꼬리 물어 저장하기
        Position position = Position.builder()
                .game(game)
                .fen(fen)
                .parentPosition(parentPosition) // ⬅️ 핵심! 가지치기 트리 구조 연결
                .moveSan(moveSan)
                .build();
        position = positionRepository.save(position);

        // 5. 분석(Analysis) 저장
        Analysis analysis = Analysis.builder()
                .position(position)
                .engineScore((String) result.get("engineScore"))
                .bestMoveUci((String) result.get("bestMoveUci"))
                .depth(depth)
                .analysisDetail((String) result.get("analysisDetail"))
                .build();
        analysisRepository.save(analysis);

        // 6. 다음 수를 위해 방금 저장한 국면의 ID를 같이 반환
        result.put("positionId", position.getPositionId());
        
        return result;
    }
}
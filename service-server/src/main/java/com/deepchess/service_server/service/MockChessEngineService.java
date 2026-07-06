package com.deepchess.service_server.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.stereotype.Service;

@Service
public class MockChessEngineService implements ChessEngineService {

    private final Random random = new Random();
    private final List<String> mockMoves = List.of("e2e4", "d2d4", "g1f3", "b1c3", "e7e5", "c7c5");

    @Override
    public Map<String, Object> analyzePosition(String fen, int depth) {
        // 실제 엔진이 돌아서 결과를 주는 것처럼 약간의 딜레이를 시뮬레이션 (0.5초)
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 가짜 점수 생성 (예: +1.25 또는 -0.40)
        double score = -2.0 + (4.0 * random.nextDouble());
        String formattedScore = String.format("%s%.2f", score >= 0 ? "+" : "", score);

        // 가짜 최선 수 선택
        String bestMove = mockMoves.get(random.nextInt(mockMoves.size()));

        Map<String, Object> result = new HashMap<>();
        result.put("engineScore", formattedScore);
        result.put("bestMoveUci", bestMove);
        result.put("depth", depth);
        result.put("analysisDetail", "{\"provider\": \"Mock Engine\", \"nodes\": 125000}");

        return result;
    }
}
package com.deepchess.service_server.service;

import java.util.Map;

public interface ChessEngineService {
    /**
     * FEN 국면을 받아 엔진 분석 결과를 반환합니다.
     * @param fen 체스 국면 표기법 (FEN)
     * @param depth 분석 깊이
     * @return engine_score, best_move 등을 담은 맵
     */
    Map<String, Object> analyzePosition(String fen, int depth);
}
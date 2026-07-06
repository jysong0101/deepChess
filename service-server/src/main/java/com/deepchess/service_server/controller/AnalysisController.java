package com.deepchess.service_server.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.deepchess.service_server.service.ChessEngineService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AnalysisController {

    private final ChessEngineService chessEngineService;

    // 예시 주소: /api/analysis?fen=startpos&depth=20
    @GetMapping("/api/analysis")
    public Map<String, Object> getAnalysis(
            @RequestParam(value = "fen", defaultValue = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1") String fen,
            @RequestParam(value = "depth", defaultValue = "20") int depth) {
        
        return chessEngineService.analyzePosition(fen, depth);
    }
}
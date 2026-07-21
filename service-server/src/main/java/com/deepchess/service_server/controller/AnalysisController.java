package com.deepchess.service_server.controller;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.deepchess.service_server.service.AnalysisRecordService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisRecordService analysisRecordService;

    @PostMapping("/api/analysis")
    public Map<String, Object> postAnalysis(
            @AuthenticationPrincipal OAuth2User oAuth2User,
            @RequestParam("gameId") Long gameId,
            @RequestParam(value = "positionId", required = false) Long positionId, // 💡 새로 추가됨!
            @RequestParam(value = "parentPositionId", required = false) Long parentPositionId,
            @RequestParam(value = "fen") String fen,
            @RequestParam(value = "moveSan", required = false) String moveSan,
            @RequestParam(value = "depth", defaultValue = "20") int depth) {
        
        if (oAuth2User == null) return Map.of("error", "로그인이 필요합니다.");
        
        // 💡 positionId도 서비스로 넘겨줍니다.
        return analysisRecordService.analyzeAndSave(gameId, positionId, parentPositionId, fen, moveSan, depth);
    }
}
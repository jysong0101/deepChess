package com.deepchess.service_server.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.deepchess.service_server.dto.request.AnalysisRequest;
import com.deepchess.service_server.dto.response.AnalysisResponse;
import com.deepchess.service_server.service.AnalysisRecordService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisRecordService analysisRecordService;

    @PostMapping("/api/analysis")
    public AnalysisResponse postAnalysis(
            @AuthenticationPrincipal OAuth2User oAuth2User,
            @ModelAttribute AnalysisRequest request) {
        if (oAuth2User == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        return analysisRecordService.analyzeAndSave(
                request.gameId(),
                request.positionId(),
                request.parentPositionId(),
                request.fen(),
                request.moveSan(),
                request.depthOrDefault());
    }
}

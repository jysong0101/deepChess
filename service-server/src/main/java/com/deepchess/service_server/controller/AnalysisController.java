package com.deepchess.service_server.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.deepchess.service_server.dto.request.AnalysisRequest;
import com.deepchess.service_server.dto.response.AnalysisResponse;
import com.deepchess.service_server.entity.User;
import com.deepchess.service_server.service.AnalysisRecordService;
import com.deepchess.service_server.service.CurrentUserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisRecordService analysisRecordService;
    private final CurrentUserService currentUserService;

    @PostMapping("/api/analysis")
    public AnalysisResponse postAnalysis(
            @AuthenticationPrincipal OAuth2User oAuth2User,
            @ModelAttribute AnalysisRequest request) {
        User user = currentUserService.requireCurrentUser(oAuth2User);

        return analysisRecordService.analyzeAndSave(
                user,
                request.gameId(),
                request.positionId(),
                request.parentPositionId(),
                request.fen(),
                request.moveSan(),
                request.depthOrDefault());
    }
}

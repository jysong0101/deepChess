package com.deepchess.service_server.dto.response;

public record AnalysisResponse(
        String engineScore,
        String bestMoveUci,
        int depth,
        String analysisDetail,
        Long positionId) {

    public AnalysisResponse withPositionId(Long newPositionId) {
        return new AnalysisResponse(engineScore, bestMoveUci, depth, analysisDetail, newPositionId);
    }
}

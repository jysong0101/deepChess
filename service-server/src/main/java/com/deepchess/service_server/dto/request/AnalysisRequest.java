package com.deepchess.service_server.dto.request;

public record AnalysisRequest(
        Long gameId,
        Long positionId,
        Long parentPositionId,
        String fen,
        String moveSan,
        Integer depth) {

    public int depthOrDefault() {
        return depth == null ? 20 : depth;
    }
}

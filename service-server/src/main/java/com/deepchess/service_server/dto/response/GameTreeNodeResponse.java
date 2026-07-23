package com.deepchess.service_server.dto.response;

public record GameTreeNodeResponse(
        Long positionId,
        Long parentPositionId,
        String fen,
        String moveSan,
        String engineScore,
        String bestMoveUci,
        String analysisDetail) {
}

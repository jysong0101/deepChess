package com.deepchess.service_server.dto.request;

public record ImportMoveRequest(
        String fen,
        String moveSan) {
}

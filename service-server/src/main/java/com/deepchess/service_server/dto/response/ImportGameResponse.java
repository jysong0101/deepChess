package com.deepchess.service_server.dto.response;

public record ImportGameResponse(
        Long gameId,
        Long lastPositionId,
        String message) {
}

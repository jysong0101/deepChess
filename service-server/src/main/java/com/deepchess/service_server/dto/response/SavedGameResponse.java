package com.deepchess.service_server.dto.response;

public record SavedGameResponse(
        Long gameId,
        String createdAt,
        String preview) {
}

package com.deepchess.service_server.dto.response;

public record SavedGameResponse(
        Long gameId,
        String title,
        String createdAt,
        String updatedAt,
        String preview) {
}

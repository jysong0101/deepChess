package com.deepchess.service_server.dto.response;

public record UpdateGameTitleResponse(
        Long gameId,
        String title,
        String message) {
}

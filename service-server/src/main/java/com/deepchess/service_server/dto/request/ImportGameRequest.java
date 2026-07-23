package com.deepchess.service_server.dto.request;

import java.util.List;

public record ImportGameRequest(
        String initialFen,
        String pgnContent,
        List<ImportMoveRequest> moves) {
}

package com.deepchess.service_server.dto.request;

public record NicknameRequest(String nickname) {

    public boolean hasNickname() {
        return nickname != null && !nickname.trim().isEmpty();
    }

    public String trimmedNickname() {
        return nickname.trim();
    }
}

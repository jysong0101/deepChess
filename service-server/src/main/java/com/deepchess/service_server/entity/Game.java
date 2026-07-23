package com.deepchess.service_server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "games")
public class Game extends BaseEntity {
    public static final int TITLE_MAX_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_id")
    private Long gameId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "pgn_content", columnDefinition = "TEXT")
    private String pgnContent;

    @Column(name = "fen_content")
    private String fenContent;

    // ddl-auto 환경에서 기존 행과 호환되도록 nullable로 추가하고 시작 시 보정한다.
    @Column(name = "title", length = TITLE_MAX_LENGTH)
    private String title;

    // 💡 추가됨: 정식 보관함에 저장되었는지 여부
    @Column(name = "is_saved", nullable = false)
    private boolean isSaved;

    @Builder
    public Game(User user, String pgnContent, String fenContent, String title, boolean isSaved) {
        this.user = user;
        this.pgnContent = pgnContent;
        this.fenContent = fenContent;
        this.title = title == null ? null : validateTitle(title);
        this.isSaved = isSaved;
    }

    public void markAsSaved() {
        markAsSaved(null);
    }

    public void markAsSaved(String requestedTitle) {
        if (requestedTitle != null) {
            this.title = validateTitle(requestedTitle);
        } else if (title == null || title.isBlank()) {
            this.title = defaultTitle(gameId);
        }
        this.isSaved = true;
    }

    public void rename(String title) {
        this.title = validateTitle(title);
    }

    public void assignDefaultTitleIfMissing() {
        if (title == null || title.isBlank()) {
            this.title = defaultTitle(gameId);
        }
    }

    private static String defaultTitle(Long gameId) {
        return "저장된 기보 #" + gameId;
    }

    private static String validateTitle(String value) {
        if (value == null) {
            throw new IllegalArgumentException("기보 제목을 입력해 주세요.");
        }

        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("기보 제목을 입력해 주세요.");
        }
        if (normalized.length() > TITLE_MAX_LENGTH) {
            throw new IllegalArgumentException("기보 제목은 100자 이하로 입력해 주세요.");
        }
        if (normalized.chars().anyMatch(character ->
                Character.isISOControl(character) || character == '\n' || character == '\r')) {
            throw new IllegalArgumentException("기보 제목에는 줄바꿈이나 제어 문자를 사용할 수 없습니다.");
        }
        return normalized;
    }
}

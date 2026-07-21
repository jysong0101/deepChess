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

    // 💡 추가됨: 정식 보관함에 저장되었는지 여부
    @Column(name = "is_saved", nullable = false)
    private boolean isSaved;

    @Builder
    public Game(User user, String pgnContent, String fenContent, boolean isSaved) {
        this.user = user;
        this.pgnContent = pgnContent;
        this.fenContent = fenContent;
        this.isSaved = isSaved; // 💡 빌더에 추가
    }

    // 💡 추가됨: 저장 버튼 클릭 시 상태를 업데이트하는 메서드
    public void markAsSaved() {
        this.isSaved = true;
    }
}
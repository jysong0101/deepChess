package com.deepchess.service_server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
@Table(name = "positions", indexes = {
    @Index(name = "idx_game_id", columnList = "game_id"),
    @Index(name = "idx_parent_position", columnList = "parent_position_id"),
    @Index(name = "idx_fen", columnList = "fen")
})
public class Position extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "position_id")
    private Long positionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_position_id")
    private Position parentPosition;

    @Column(name = "move_uci", length = 10)
    private String moveUci;

    @Column(nullable = false)
    private String fen;

    // ⬇️ 필드를 위쪽으로 올렸습니다.
    @Column(name = "MOVE_SAN")
    private String moveSan; 

    // ⬇️ 생성자 파라미터와 내부에 moveSan을 추가했습니다!
    @Builder
    public Position(Game game, Position parentPosition, String moveUci, String fen, String moveSan) {
        this.game = game;
        this.parentPosition = parentPosition;
        this.moveUci = moveUci;
        this.fen = fen;
        this.moveSan = moveSan;
    }
}
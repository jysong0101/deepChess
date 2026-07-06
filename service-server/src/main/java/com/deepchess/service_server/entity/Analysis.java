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
@Table(name = "analysis")
public class Analysis extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_id")
    private Long analysisId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id", nullable = false)
    private Position position;

    @Column(nullable = false)
    private Integer depth;

    @Column(name = "engine_score", nullable = false, length = 50)
    private String engineScore;

    @Column(name = "best_move_uci", length = 10)
    private String bestMoveUci;

    @Column(name = "analysis_detail", columnDefinition = "TEXT")
    private String analysisDetail;

    @Builder
    public Analysis(Position position, Integer depth, String engineScore, String bestMoveUci, String analysisDetail) {
        this.position = position;
        this.depth = depth;
        this.engineScore = engineScore;
        this.bestMoveUci = bestMoveUci;
        this.analysisDetail = analysisDetail;
    }
}
package com.deepchess.service_server.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.deepchess.service_server.entity.Analysis; // 💡 꼭 추가해 주세요!

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {
    Optional<Analysis> findByPosition_PositionId(Long positionId);

    long countByPosition_Game_GameId(Long gameId);

    @Modifying(flushAutomatically = true)
    @Query("delete from Analysis a where a.position.game.gameId = :gameId")
    int deleteByPositionGameId(@Param("gameId") Long gameId);
}

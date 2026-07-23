package com.deepchess.service_server.repository;

import com.deepchess.service_server.entity.Position;
import com.deepchess.service_server.entity.User;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PositionRepository extends JpaRepository<Position, Long> {
    List<Position> findByGame_GameId(Long gameId);

    Optional<Position> findByPositionIdAndGame_GameIdAndGame_User(
            Long positionId,
            Long gameId,
            User user);

    @Modifying(flushAutomatically = true)
    @Query("update Position p set p.parentPosition = null where p.game.gameId = :gameId")
    int clearParentReferencesByGameId(@Param("gameId") Long gameId);

    @Modifying(flushAutomatically = true)
    @Query("delete from Position p where p.game.gameId = :gameId")
    int deleteByGameId(@Param("gameId") Long gameId);
}

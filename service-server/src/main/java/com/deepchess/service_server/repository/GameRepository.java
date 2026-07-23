package com.deepchess.service_server.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.deepchess.service_server.entity.Game;
import com.deepchess.service_server.entity.User;

public interface GameRepository extends JpaRepository<Game, Long> {
    List<Game> findByUserAndIsSavedTrue(User user, Sort sort);

    List<Game> findByUserAndIsSavedTrueAndTitleContainingIgnoreCase(User user, String title, Sort sort);

    @Query("select g from Game g where g.isSaved = true and (g.title is null or trim(g.title) = '')")
    List<Game> findSavedGamesWithoutTitle();

    Optional<Game> findByGameIdAndUser(Long gameId, User user);

    Optional<Game> findByGameIdAndUserAndIsSavedTrue(Long gameId, User user);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Game g where g.gameId = :gameId")
    int deleteByGameId(@Param("gameId") Long gameId);
}

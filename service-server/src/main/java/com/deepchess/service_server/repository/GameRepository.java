package com.deepchess.service_server.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.deepchess.service_server.entity.Game;
import com.deepchess.service_server.entity.User;

public interface GameRepository extends JpaRepository<Game, Long> {
    List<Game> findByUserAndIsSavedTrueOrderByCreatedAtDesc(User user);
}
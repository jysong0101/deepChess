package com.deepchess.service_server.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.deepchess.service_server.entity.Game;

public interface GameRepository extends JpaRepository<Game, Long> {
}
package com.deepchess.service_server.repository;

import com.deepchess.service_server.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PositionRepository extends JpaRepository<Position, Long> {
    // ⬇️ 게임 ID로 모든 국면을 찾아오는 메서드 추가
    List<Position> findByGame_GameId(Long gameId);
}   
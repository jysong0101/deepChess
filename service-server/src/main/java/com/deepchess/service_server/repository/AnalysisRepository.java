package com.deepchess.service_server.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.deepchess.service_server.entity.Analysis; // 💡 꼭 추가해 주세요!

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {
    // 💡 추가됨: Position ID로 기존 Analysis 객체를 찾아오는 기능
    Optional<Analysis> findByPosition_PositionId(Long positionId);
}
package com.deepchess.service_server.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.deepchess.service_server.entity.Analysis;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {
}
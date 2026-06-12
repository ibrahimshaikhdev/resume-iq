package com.ai.Resume.analyser.repository;

import com.ai.Resume.analyser.model.AnonymousAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnonymousAnalysisRepository extends JpaRepository<AnonymousAnalysis, Long> {
    Optional<AnonymousAnalysis> findByContentHash(String contentHash);
}

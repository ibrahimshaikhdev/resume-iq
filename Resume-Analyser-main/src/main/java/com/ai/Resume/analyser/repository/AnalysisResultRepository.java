package com.ai.Resume.analyser.repository;

import com.ai.Resume.analyser.model.AnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {
    List<AnalysisResult> findByEmailOrderByAnalyzedAtDesc(String email);
    Optional<AnalysisResult> findFirstByEmailOrderByAnalyzedAtDesc(String email);
    void deleteByEmail(String email);
}

package com.ai.Resume.analyser.repository;

import com.ai.Resume.analyser.model.JdMatchResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JdMatchResultRepository extends JpaRepository<JdMatchResult, Long> {
    Optional<JdMatchResult> findFirstByEmailOrderByMatchPercentageDesc(String email);
    List<JdMatchResult> findByEmailOrderByMatchedAtDesc(String email);
}

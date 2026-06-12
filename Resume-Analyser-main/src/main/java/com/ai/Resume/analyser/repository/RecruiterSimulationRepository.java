package com.ai.Resume.analyser.repository;

import com.ai.Resume.analyser.model.RecruiterSimulation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecruiterSimulationRepository extends JpaRepository<RecruiterSimulation, Long> {

    List<RecruiterSimulation> findByEmailOrderBySimulatedAtDesc(String email);

    Optional<RecruiterSimulation> findFirstByEmailOrderBySimulatedAtDesc(String email);
}

package com.ai.Resume.analyser.repository;

import com.ai.Resume.analyser.model.AnalysisJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnalysisJobRepository extends JpaRepository<AnalysisJob, Long> {

    List<AnalysisJob> findByEmailOrderByCreatedAtDesc(String email);

    Optional<AnalysisJob> findFirstByEmailAndJobTypeOrderByCreatedAtDesc(String email, String jobType);

    List<AnalysisJob> findByStatus(AnalysisJob.JobStatus status);

    Optional<AnalysisJob> findByIdAndEmail(Long id, String email);
}

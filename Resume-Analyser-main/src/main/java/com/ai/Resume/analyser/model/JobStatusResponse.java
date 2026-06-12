package com.ai.Resume.analyser.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JobStatusResponse {

    private Long jobId;
    private String status;
    private String jobType;
    private String roles;
    private String message;
    private String errorMessage;
    private int retryCount;
    private int maxRetries;
    private Date createdAt;
    private Date startedAt;
    private Date completedAt;
    private Date updatedAt;
    private Long resultId;
    private AnalysisResultDto result;
}

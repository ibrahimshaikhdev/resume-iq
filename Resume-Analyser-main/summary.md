# ResumeIQ

## Project Transformation Directive

This project was initially cloned from an external open-source repository and is being used strictly as a technical starting point.

The objective is to transform it into a fully original, production-grade software product owned and developed by our team.

This must not remain visually, structurally, or architecturally recognizable as the original source repository.

---

## New Project Ownership

Developed By:

- Ibrahim Shaikh
- FRIEND_1
- FRIEND_2

---

## Mandatory Transformation Requirements

### Remove Existing Attribution

Identify and remove all traces of previous ownership:

- original author names
- GitHub usernames
- footer credits
- repository branding
- logos
- comments referencing previous authors where unnecessary
- project titles
- metadata references

---

### Replace Footer Across Entire Application

All pages must include:

Built with ❤️ by Ibrahim Shaikh, Vaibhav Koul, and Vishvesh Rajulwar

---

### Full Visual Redesign Required

Completely redesign:

- Landing page
- Authentication pages
- Dashboard
- Resume upload section
- Analysis results screen
- Analytics charts
- Navigation
- Footer
- Cards and layouts

The final interface must look like a modern SaaS platform.

---

### Architecture Refactoring

Refactor:

Frontend:

- component hierarchy
- folder structure
- reusable UI system
- API abstraction

Backend:

- controller/service/repository separation
- DTO structure
- exception handling
- validation architecture

Database:

- normalized schema
- optimized relationships

---

# Project Overview

ResumeIQ is an enterprise-grade AI-powered ATS resume intelligence platform built using React.js, Spring Boot, and MySQL.

It helps candidates optimize resumes against specific job descriptions through intelligent analysis, semantic matching, scoring, and career recommendations.

This is not a basic resume checker.

It is a professional career intelligence system.

---

# Tech Stack

## Frontend

- React.js
- Tailwind CSS
- Axios
- Chart.js

## Backend

- Spring Boot
- Spring Security
- JWT Authentication
- REST APIs

## Database

- MySQL

## Document Processing

- PDF parsing
- DOCX extraction

---

# Core Features

## 1. Smart Resume Upload

Users can upload:

- PDF
- DOCX

Validation required.

---

## 2. ATS Intelligence Engine

Advanced scoring based on:

- keyword matching
- formatting quality
- semantic relevance
- experience alignment
- skill completeness

Outputs:

- ATS score
- breakdown report

---

## 3. Job Description Matching

Users paste a job description.

System compares:

- required skills
- experience requirements
- keyword alignment

Provides:

- match percentage
- missing requirements

---

## 4. Skill Gap Heatmap

Interactive visual comparison between:

Candidate Skills vs Job Requirements

---

## 5. Resume Version Analytics

Track:

- uploaded versions
- historical score improvements
- optimization trends

---

## 6. AI Career Recommendation Engine

Suggest:

- suitable roles
- missing skills
- improvement roadmap
- learning recommendations

---

## 7. Downloadable Analysis Report

Generate professional PDF reports containing:

- ATS score
- recommendations
- skill gaps
- optimization suggestions

---

## 8. User Dashboard

Professional analytics dashboard with:

- score trends
- resume history
- comparison charts
- recommendations

---

# Authentication Module

Implement:

- User registration
- Login
- JWT authentication
- Session persistence
- Secure endpoints

---

# UI Design Vision

Modern premium SaaS style.

Inspiration:
Enterprise HR analytics platforms.

Must include:

- dark/light mode
- smooth animations
- clean cards
- responsive layouts

---

# Implementation Phases

## Phase 1

Project cleanup + UI modernization

Tasks:

- remove old branding
- redesign homepage
- improve dashboard UI
- modern responsive design

---

## Phase 2

Backend architecture refactor

Tasks:

- clean service layers
- DTO redesign
- validation
- exception handling

---

## Phase 3

ATS intelligence improvements

Tasks:

- scoring engine
- skill gap heatmap
- job description matching

---

## Phase 4

Analytics + reports

Tasks:

- score history
- PDF export
- trend visualizations

# Phase 5 — Asynchronous Processing Engine: COMPLETED

Purpose: make analysis architecture feel production-grade.

Implemented:

- AnalysisJob entity with lifecycle states (QUEUED → PROCESSING → SCORING → RECOMMENDATIONS → COMPLETED / FAILED)
- AsyncAnalysisService with @Async methods for background resume analysis
- AnalysisJobRepository for job persistence and status queries
- AsyncConfig with ThreadPoolTaskExecutor (3 core, 6 max, 20 queue capacity)
- Job status polling endpoint: GET /jobs/{id}/status
- Async job submission endpoints: POST /jobs/analyze, POST /jobs/matchJd
- JobProgress.jsx frontend component with real-time stage visualization
- Retry mechanism (up to 3 attempts with error tracking)
- Failure recovery with error messages and retry count
- Processing timeout management
- Backward-compatible sync endpoints preserved

Flow:

Upload → QUEUED → PROCESSING → SCORING → RECOMMENDATIONS → COMPLETED

Why recruiters care:

This shows backend architecture maturity.

It signals:

“Understands scalable processing systems.”

Huge jump.

# Phase 6 — Performance Optimization Layer

Implement:

MySQL query optimization
indexing strategy
response caching
duplicate analysis result reuse
optimized document parsing
benchmark measurement dashboard

Add visible metrics like:

average processing latency
score generation time
cache hit rate

Why it matters:

Senior engineers think in performance.

This phase shows that.

# Phase 7 — Observability & Monitoring

Implement:

structured backend logging
request tracing
analysis execution logs
processing metrics
exception monitoring dashboard
API health monitoring

Dashboard examples:

successful analyses
failed jobs
average response time
error trends

Recruiter signal:

“Understands production reliability.”

Very strong.

# Phase 8 — Intelligence Engine v2

Upgrade your ATS logic.

Add:

Multi-dimensional weighted scoring

Break ATS score into:

semantic relevance
technical skill coverage
experience alignment
achievement quality
keyword density
formatting confidence

Add score explainability.

Example:

“Backend expertise underrepresented for this role.”

That feels enterprise-grade.

# Phase 9 — Recruiter Simulation Layer

This can become your standout feature.

Simulate evaluation styles for:

startup recruiter
enterprise recruiter
product company recruiter
engineering manager

Output:

“How each reviewer would score this resume.”

This is memorable in interviews.

# Phase 10 — Deployment & Reliability Engineering

Must have.

Add:

Dockerization
CI/CD pipeline
automated testing
deployment monitoring
rollback support
production health checks

This proves shipping capability.

---

# Important Development Rule

Implement one phase at a time.

Do NOT attempt full-system rewrites.

Preserve working functionality while incrementally transforming the codebase.

---

# Final Objective

ResumeIQ must present as a fully original enterprise-grade product developed by:

Ibrahim Shaikh  
Vaibhav Koul  
Vishvesh Rajulwar

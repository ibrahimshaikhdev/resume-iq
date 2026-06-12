CREATE TABLE IF NOT EXISTS anonymous_analysis (
    id BIGINT NOT NULL AUTO_INCREMENT,
    content_hash VARCHAR(64) NOT NULL,
    roles VARCHAR(255) NOT NULL,
    score INT NOT NULL,
    atsoptimizationscore INT NOT NULL,
    atsbreakdown TEXT,
    dimension_scores TEXT,
    dimension_explanations TEXT,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_anonymous_analysis_content_hash (content_hash),
    KEY idx_anon_hash (content_hash)
);

CREATE TABLE IF NOT EXISTS anonymous_analysis_pros (
    anonymous_analysis_id BIGINT NOT NULL,
    pros VARCHAR(450),
    KEY idx_anonymous_analysis_pros_id (anonymous_analysis_id),
    CONSTRAINT fk_anonymous_analysis_pros
        FOREIGN KEY (anonymous_analysis_id)
        REFERENCES anonymous_analysis (id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS anonymous_analysis_cons (
    anonymous_analysis_id BIGINT NOT NULL,
    cons VARCHAR(450),
    KEY idx_anonymous_analysis_cons_id (anonymous_analysis_id),
    CONSTRAINT fk_anonymous_analysis_cons
        FOREIGN KEY (anonymous_analysis_id)
        REFERENCES anonymous_analysis (id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS anonymous_analysis_suggestions (
    anonymous_analysis_id BIGINT NOT NULL,
    suggestions VARCHAR(450),
    KEY idx_anonymous_analysis_suggestions_id (anonymous_analysis_id),
    CONSTRAINT fk_anonymous_analysis_suggestions
        FOREIGN KEY (anonymous_analysis_id)
        REFERENCES anonymous_analysis (id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS anonymous_analysis_matched_skills (
    anonymous_analysis_id BIGINT NOT NULL,
    matched_skills VARCHAR(300),
    KEY idx_anonymous_analysis_matched_skills_id (anonymous_analysis_id),
    CONSTRAINT fk_anonymous_analysis_matched_skills
        FOREIGN KEY (anonymous_analysis_id)
        REFERENCES anonymous_analysis (id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS anonymous_analysis_missing_skills (
    anonymous_analysis_id BIGINT NOT NULL,
    missing_skills VARCHAR(300),
    KEY idx_anonymous_analysis_missing_skills_id (anonymous_analysis_id),
    CONSTRAINT fk_anonymous_analysis_missing_skills
        FOREIGN KEY (anonymous_analysis_id)
        REFERENCES anonymous_analysis (id)
        ON DELETE CASCADE
);
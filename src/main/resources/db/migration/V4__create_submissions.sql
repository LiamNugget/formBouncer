CREATE TABLE submissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    api_key_id BIGINT NOT NULL,
    score DECIMAL(4,3) NOT NULL,
    is_spam BOOLEAN NOT NULL,
    threshold_used DECIMAL(3,2) NOT NULL,
    reasons JSON,
    ip_address VARCHAR(45),
    user_agent VARCHAR(512),
    timing_seconds INT,
    reported_as VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_submissions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_submissions_api_key FOREIGN KEY (api_key_id) REFERENCES api_keys (id) ON DELETE CASCADE
);

CREATE INDEX idx_submissions_user_id ON submissions (user_id);
CREATE INDEX idx_submissions_created_at ON submissions (created_at);
CREATE INDEX idx_submissions_is_spam ON submissions (is_spam);

CREATE TABLE mydata_snapshot (
    user_id BIGINT NOT NULL PRIMARY KEY,
    payload JSON NOT NULL,
    schema_version INT NOT NULL,
    source_fetched_at DATETIME(6) NOT NULL,
    synced_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_mydata_snapshot_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE mydata_sync_job (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    active_user_id BIGINT GENERATED ALWAYS AS (
        CASE
            WHEN status IN ('QUEUED', 'RUNNING', 'RETRY_WAIT', 'PERSIST_RETRY') THEN user_id
            ELSE NULL
        END
    ) STORED,
    status VARCHAR(30) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME(6) NOT NULL,
    worker_id VARCHAR(100) NULL,
    error_code VARCHAR(100) NULL,
    error_message VARCHAR(1000) NULL,
    created_at DATETIME(6) NOT NULL,
    started_at DATETIME(6) NULL,
    completed_at DATETIME(6) NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_mydata_sync_job_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT uk_mydata_sync_job_active_user UNIQUE (active_user_id),
    INDEX idx_mydata_sync_job_dispatch (status, next_retry_at)
);

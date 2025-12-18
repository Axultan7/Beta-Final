-- Request Service Database Initialization

-- Create processed_requests table
CREATE TABLE IF NOT EXISTS processed_requests (
    id VARCHAR(36) PRIMARY KEY,
    original_request_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    category INTEGER NOT NULL,
    original_message VARCHAR(2000) NOT NULL,
    response_message VARCHAR(2000),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_processed_requests_original_id ON processed_requests(original_request_id);
CREATE INDEX IF NOT EXISTS idx_processed_requests_user_id ON processed_requests(user_id);
CREATE INDEX IF NOT EXISTS idx_processed_requests_status ON processed_requests(status);
CREATE INDEX IF NOT EXISTS idx_processed_requests_created_at ON processed_requests(created_at DESC);

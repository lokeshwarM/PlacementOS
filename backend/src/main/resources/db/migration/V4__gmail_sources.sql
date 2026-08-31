CREATE TABLE gmail_sources (
    id BIGSERIAL PRIMARY KEY,
    email_address VARCHAR(255) UNIQUE NOT NULL,
    provider VARCHAR(50) NOT NULL,
    credential TEXT,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_gmail_sources_email ON gmail_sources(email_address);
CREATE INDEX idx_gmail_sources_status ON gmail_sources(status);

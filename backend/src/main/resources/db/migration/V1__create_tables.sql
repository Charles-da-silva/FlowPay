CREATE TABLE attendants (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL,
    max_simultaneous_customers INTEGER NOT NULL DEFAULT 3
);

CREATE TABLE attendant_categories (
    attendant_id BIGINT NOT NULL REFERENCES attendants(id) ON DELETE CASCADE,
    category VARCHAR(40) NOT NULL,
    PRIMARY KEY (attendant_id, category)
);

CREATE TABLE service_requests (
    id BIGSERIAL PRIMARY KEY,
    customer_name VARCHAR(120) NOT NULL,
    category VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    attendant_id BIGINT NULL REFERENCES attendants(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NULL,
    finished_at TIMESTAMP WITH TIME ZONE NULL
);

CREATE INDEX idx_service_requests_status_category_created
    ON service_requests(status, category, created_at);

ALTER TABLE attendants
    ADD COLUMN paused_since TIMESTAMP WITH TIME ZONE NULL;

CREATE TABLE attendant_pauses (
    id BIGSERIAL PRIMARY KEY,
    attendant_id BIGINT NOT NULL REFERENCES attendants(id) ON DELETE CASCADE,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at TIMESTAMP WITH TIME ZONE NULL
);

CREATE INDEX idx_attendant_pauses_attendant_started
    ON attendant_pauses(attendant_id, started_at);

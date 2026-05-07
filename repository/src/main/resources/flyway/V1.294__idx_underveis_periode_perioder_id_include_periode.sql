DROP INDEX IF EXISTS idx_underveis_periode_perioder_id;
CREATE INDEX idx_underveis_periode_perioder_id ON underveis_periode (perioder_id) INCLUDE (periode);


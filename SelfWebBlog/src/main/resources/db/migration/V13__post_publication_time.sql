ALTER TABLE post ADD COLUMN published_time TIMESTAMP;

UPDATE post
SET published_time = COALESCE(create_time, update_time, CURRENT_TIMESTAMP)
WHERE status IN ('PUBLISHED', 'ARCHIVED');

CREATE INDEX IF NOT EXISTS idx_post_status_published_time
    ON post(status, published_time DESC, id DESC);

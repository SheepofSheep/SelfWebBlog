ALTER TABLE post ADD COLUMN background_mode TEXT NOT NULL DEFAULT 'COVER';
ALTER TABLE post ADD COLUMN background_value TEXT DEFAULT '';
ALTER TABLE post ADD COLUMN background_focus_x INTEGER NOT NULL DEFAULT 50;
ALTER TABLE post ADD COLUMN background_focus_y INTEGER NOT NULL DEFAULT 50;

CREATE INDEX IF NOT EXISTS idx_post_status_create_time
    ON post(status, create_time DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_post_status_update_time
    ON post(status, update_time DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_comment_status_create_time
    ON comment(status, create_time DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_security_event_create_time
    ON security_event(create_time DESC, id DESC);

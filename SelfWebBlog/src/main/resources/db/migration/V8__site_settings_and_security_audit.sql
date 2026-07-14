ALTER TABLE blog_info ADD COLUMN site_start_date TEXT;
ALTER TABLE blog_info ADD COLUMN current_status TEXT DEFAULT '';
ALTER TABLE blog_info ADD COLUMN status_updated_time TIMESTAMP;
ALTER TABLE blog_info ADD COLUMN about_markdown TEXT DEFAULT '';
ALTER TABLE blog_info ADD COLUMN social_links TEXT DEFAULT '{}';

UPDATE blog_info
SET site_start_date = COALESCE(
    site_start_date,
    (SELECT date(MIN(create_time)) FROM post),
    date('now')
)
WHERE id = 1;

CREATE TABLE IF NOT EXISTS security_event (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    actor_user_id INTEGER,
    action TEXT NOT NULL,
    target_type TEXT DEFAULT '',
    target_id TEXT DEFAULT '',
    detail TEXT DEFAULT '',
    ip_hash TEXT DEFAULT '',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_security_event_action_time
    ON security_event(action, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_security_event_actor_time
    ON security_event(actor_user_id, create_time DESC);

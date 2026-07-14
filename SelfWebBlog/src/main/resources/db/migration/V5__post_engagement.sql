ALTER TABLE post ADD COLUMN like_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE post ADD COLUMN view_count INTEGER NOT NULL DEFAULT 0;

CREATE TABLE interaction_like (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    target_type TEXT NOT NULL CHECK (target_type IN ('POST', 'COMMENT')),
    target_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (target_type, target_id, user_id)
);

CREATE INDEX idx_interaction_like_user ON interaction_like(user_id, create_time DESC);

CREATE TABLE post_view_daily (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    post_id INTEGER NOT NULL,
    visitor_hash TEXT NOT NULL,
    view_date TEXT NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (post_id, visitor_hash, view_date)
);

CREATE INDEX idx_post_view_daily_date ON post_view_daily(view_date, post_id);

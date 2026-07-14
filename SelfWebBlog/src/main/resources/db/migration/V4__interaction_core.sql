CREATE TABLE comment_v4 (
    id INTEGER PRIMARY KEY,
    post_id INTEGER,
    target_type TEXT NOT NULL DEFAULT 'POST' CHECK (target_type IN ('POST', 'GUESTBOOK')),
    target_id INTEGER NOT NULL,
    root_id INTEGER,
    reply_to_id INTEGER,
    reply_to_user_id INTEGER,
    content TEXT NOT NULL,
    user_id INTEGER,
    nickname TEXT NOT NULL,
    guest_name TEXT,
    avatar_url TEXT,
    role TEXT NOT NULL DEFAULT 'USER',
    pinned INTEGER NOT NULL DEFAULT 0,
    title_name TEXT DEFAULT '',
    title_style TEXT DEFAULT 'default',
    status TEXT NOT NULL DEFAULT 'PUBLISHED'
        CHECK (status IN ('PENDING', 'PUBLISHED', 'HIDDEN', 'SPAM', 'DELETED')),
    ip_ciphertext TEXT,
    ip_hash TEXT,
    ip_region TEXT,
    like_count INTEGER NOT NULL DEFAULT 0 CHECK (like_count >= 0),
    edited_time TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO comment_v4 (
    id, post_id, target_type, target_id, content, user_id, nickname, avatar_url,
    role, pinned, title_name, title_style, status, like_count, create_time
)
SELECT
    id, post_id, 'POST', post_id, content, user_id, nickname, avatar_url,
    role, pinned, title_name, title_style, 'PUBLISHED', 0, create_time
FROM comment;

DROP TABLE comment;
ALTER TABLE comment_v4 RENAME TO comment;

CREATE INDEX idx_comment_target_status_time
    ON comment(target_type, target_id, status, pinned DESC, create_time DESC);
CREATE INDEX idx_comment_root_time ON comment(root_id, create_time ASC);
CREATE INDEX idx_comment_user_id ON comment(user_id);
CREATE INDEX idx_comment_ip_hash_status ON comment(ip_hash, status);


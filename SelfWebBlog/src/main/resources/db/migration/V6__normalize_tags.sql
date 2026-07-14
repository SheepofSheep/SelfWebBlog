CREATE TABLE IF NOT EXISTS post_tag (
    post_id INTEGER NOT NULL,
    tag_id INTEGER NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (post_id, tag_id),
    FOREIGN KEY (post_id) REFERENCES post(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tag(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_post_tag_tag_id ON post_tag(tag_id, post_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_tag_name_nocase ON tag(name COLLATE NOCASE);
CREATE UNIQUE INDEX IF NOT EXISTS uq_tag_slug_nocase ON tag(slug COLLATE NOCASE);

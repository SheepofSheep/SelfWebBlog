CREATE TABLE IF NOT EXISTS image_asset (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    content_hash TEXT NOT NULL,
    purpose TEXT NOT NULL,
    original_path TEXT NOT NULL,
    small_webp_path TEXT,
    large_webp_path TEXT,
    original_mime TEXT NOT NULL,
    width INTEGER NOT NULL,
    height INTEGER NOT NULL,
    byte_size INTEGER NOT NULL,
    uploader_id INTEGER NOT NULL,
    reference_status TEXT NOT NULL DEFAULT 'ACTIVE',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_image_asset_owner_hash_purpose
    ON image_asset(uploader_id, content_hash, purpose);
CREATE INDEX IF NOT EXISTS idx_image_asset_reference
    ON image_asset(reference_status, create_time DESC);

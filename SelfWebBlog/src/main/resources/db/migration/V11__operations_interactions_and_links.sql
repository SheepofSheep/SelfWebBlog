ALTER TABLE post ADD COLUMN archived_time TIMESTAMP;

ALTER TABLE comment ADD COLUMN pin_order INTEGER;
ALTER TABLE comment ADD COLUMN pinned_at TIMESTAMP;

UPDATE comment
SET pin_order = (
        SELECT COUNT(*) - 1
        FROM comment AS earlier
        WHERE earlier.target_type = comment.target_type
          AND earlier.target_id = comment.target_id
          AND earlier.pinned = 1
          AND (
              earlier.create_time < comment.create_time
              OR (earlier.create_time = comment.create_time AND earlier.id <= comment.id)
          )
    ),
    pinned_at = COALESCE(create_time, CURRENT_TIMESTAMP)
WHERE pinned = 1;

CREATE INDEX IF NOT EXISTS idx_post_status_archived_time
    ON post(status, archived_time DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_interaction_pin_order
    ON comment(target_type, target_id, pinned DESC, pin_order, create_time DESC, id DESC);

CREATE TABLE IF NOT EXISTS site_link (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    label TEXT NOT NULL,
    url TEXT NOT NULL,
    icon_key TEXT NOT NULL DEFAULT 'link',
    enabled INTEGER NOT NULL DEFAULT 1 CHECK (enabled IN (0, 1)),
    sort_order INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_site_link_enabled_order
    ON site_link(enabled, sort_order, id);

WITH legacy_link(label, raw_url, icon_key, preferred_order) AS (
    SELECT 'GitHub', json_extract(social_links, '$.github'), 'github', 0
    FROM blog_info
    WHERE id = 1 AND json_valid(COALESCE(social_links, '{}'))
    UNION ALL
    SELECT '哔哩哔哩', json_extract(social_links, '$.bilibili'), 'bilibili', 1
    FROM blog_info
    WHERE id = 1 AND json_valid(COALESCE(social_links, '{}'))
    UNION ALL
    SELECT '个人站点', json_extract(social_links, '$.website'), 'website', 2
    FROM blog_info
    WHERE id = 1 AND json_valid(COALESCE(social_links, '{}'))
    UNION ALL
    SELECT '邮箱', json_extract(social_links, '$.email'), 'mail', 3
    FROM blog_info
    WHERE id = 1 AND json_valid(COALESCE(social_links, '{}'))
),
normalized AS (
    SELECT
        label,
        CASE
            WHEN icon_key = 'mail' AND lower(trim(raw_url)) NOT LIKE 'mailto:%'
                THEN 'mailto:' || trim(raw_url)
            ELSE trim(raw_url)
        END AS url,
        icon_key,
        preferred_order
    FROM legacy_link
    WHERE raw_url IS NOT NULL AND trim(raw_url) <> ''
),
ordered AS (
    SELECT
        label,
        url,
        icon_key,
        ROW_NUMBER() OVER (ORDER BY preferred_order) - 1 AS sort_order
    FROM normalized
)
INSERT INTO site_link (label, url, icon_key, enabled, sort_order)
SELECT label, url, icon_key, 1, sort_order
FROM ordered;

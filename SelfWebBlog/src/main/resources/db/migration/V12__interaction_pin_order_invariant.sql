UPDATE comment
SET pinned = 0,
    pin_order = NULL,
    pinned_at = NULL
WHERE pinned = 1
  AND (root_id IS NOT NULL OR status <> 'PUBLISHED');

WITH ordered AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY target_type, target_id
            ORDER BY
                CASE WHEN pin_order IS NULL THEN 1 ELSE 0 END,
                pin_order,
                COALESCE(pinned_at, create_time),
                id
        ) - 1 AS compact_order
    FROM comment
    WHERE pinned = 1 AND root_id IS NULL AND status = 'PUBLISHED'
)
UPDATE comment
SET pin_order = (
    SELECT compact_order FROM ordered WHERE ordered.id = comment.id
)
WHERE id IN (SELECT id FROM ordered);

CREATE UNIQUE INDEX IF NOT EXISTS uq_comment_target_pin_order
    ON comment(target_type, target_id, pin_order)
    WHERE pinned = 1 AND root_id IS NULL;

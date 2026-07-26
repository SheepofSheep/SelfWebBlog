CREATE TRIGGER IF NOT EXISTS trg_comment_pinned_order_insert
BEFORE INSERT ON comment
WHEN NEW.pinned = 1
 AND (NEW.pin_order IS NULL OR NEW.root_id IS NOT NULL OR NEW.status <> 'PUBLISHED')
BEGIN
    SELECT RAISE(ABORT, 'pinned interaction requires a published root with pin_order');
END;

CREATE TRIGGER IF NOT EXISTS trg_comment_pinned_order_update
BEFORE UPDATE OF pinned, pin_order, root_id, status ON comment
WHEN NEW.pinned = 1
 AND (NEW.pin_order IS NULL OR NEW.root_id IS NOT NULL OR NEW.status <> 'PUBLISHED')
BEGIN
    SELECT RAISE(ABORT, 'pinned interaction requires a published root with pin_order');
END;

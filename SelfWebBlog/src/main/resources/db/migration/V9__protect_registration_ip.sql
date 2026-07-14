ALTER TABLE user ADD COLUMN registration_ip_hash TEXT DEFAULT '';
CREATE INDEX IF NOT EXISTS idx_user_registration_ip_hash ON user(registration_ip_hash);

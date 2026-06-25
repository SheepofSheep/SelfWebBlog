CREATE TABLE IF NOT EXISTS blog_info (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    avatar_url TEXT,
    nickname TEXT,
    bio TEXT,
    bg_url TEXT
);

CREATE TABLE IF NOT EXISTS post (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    content TEXT,
    image_url TEXT,
    summary TEXT DEFAULT '',
    cover_url TEXT DEFAULT '',
    category TEXT DEFAULT '',
    tags TEXT DEFAULT '',
    status TEXT DEFAULT 'PUBLISHED',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS comment (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    post_id INTEGER NOT NULL,
    content TEXT NOT NULL,
    nickname TEXT NOT NULL,
    avatar_url TEXT,
    role TEXT NOT NULL,
    pinned INTEGER DEFAULT 0,
    title_name TEXT DEFAULT '',
    title_style TEXT DEFAULT 'default',
    user_id INTEGER,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password TEXT,
    email TEXT,
    avatar_url TEXT,
    role TEXT NOT NULL DEFAULT 'USER',
    github_id TEXT UNIQUE,
    nickname TEXT DEFAULT '',
    ip_address TEXT DEFAULT '',
    title_name TEXT DEFAULT '',
    title_style TEXT DEFAULT 'default',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tag (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    slug TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_post_status ON post(status);
CREATE INDEX IF NOT EXISTS idx_post_category ON post(category);
CREATE INDEX IF NOT EXISTS idx_comment_post_id ON comment(post_id);
CREATE INDEX IF NOT EXISTS idx_comment_user_id ON comment(user_id);

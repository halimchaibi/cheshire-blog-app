-- 1. Reset Schema (Clean slate for Docker init)
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;

-- 2. Enable UUID Support
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 3. Create Trigger Function for 'updated_at' behavior
CREATE OR REPLACE FUNCTION update_modified_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 4. Create Tables
CREATE TABLE Authors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE Articles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    content TEXT,
    publish_date TIMESTAMP,
    is_published BOOLEAN DEFAULT FALSE,
    author_id UUID REFERENCES Authors(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE Comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    article_id UUID REFERENCES Articles(id) ON DELETE CASCADE,
    author_name VARCHAR(255),
    author_email VARCHAR(255),
    content TEXT,
    comment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. Attach Auto-Update Trigger
CREATE TRIGGER update_articles_modtime
    BEFORE UPDATE ON Articles
    FOR EACH ROW
    EXECUTE PROCEDURE update_modified_column();

-- 6. Insert Authors (Letting Postgres generate UUIDs automatically)
INSERT INTO Authors (username, email) VALUES
('john_doe', 'john@example.com'),
('jane_smith', 'jane@example.com'),
('bob_writer', 'bob@example.com'),
('alice_author', 'alice@example.com'),
('charlie_blogger', 'charlie@example.com'),
('diana_pen', 'diana@example.com'),
('evan_words', 'evan@example.com'),
('fiona_scribe', 'fiona@example.com');

-- 7. Insert Articles
INSERT INTO Articles (title, content, publish_date, is_published, author_id) VALUES
('Getting Started with SQL', 'SQL basics...', NOW() - INTERVAL '10 days', TRUE, (SELECT id FROM Authors WHERE username = 'john_doe')),
('Database Design Best Practices', 'Designing...', NOW() - INTERVAL '8 days', TRUE, (SELECT id FROM Authors WHERE username = 'john_doe')),
('The Future of Web Development', 'Trends...', NOW() - INTERVAL '7 days', TRUE, (SELECT id FROM Authors WHERE username = 'jane_smith')),
('Introduction to React Hooks', 'Hooks...', NOW() - INTERVAL '5 days', TRUE, (SELECT id FROM Authors WHERE username = 'jane_smith')),
('Python for Data Science', 'Data science...', NOW() - INTERVAL '6 days', TRUE, (SELECT id FROM Authors WHERE username = 'bob_writer')),
('Machine Learning Basics', 'ML intro...', NOW() - INTERVAL '3 days', TRUE, (SELECT id FROM Authors WHERE username = 'bob_writer')),
('Building RESTful APIs', 'API design...', NOW() - INTERVAL '4 days', TRUE, (SELECT id FROM Authors WHERE username = 'alice_author')),
('Advanced Database Optimization', 'Draft...', NULL, FALSE, (SELECT id FROM Authors WHERE username = 'john_doe')),
('Upcoming JavaScript Features', 'Draft...', NULL, FALSE, (SELECT id FROM Authors WHERE username = 'jane_smith'));

-- 8. Insert Comments
INSERT INTO Comments (article_id, author_name, author_email, content, comment_date) VALUES
((SELECT id FROM Articles WHERE title = 'Getting Started with SQL'), 'Mike Learner', 'mike@learner.com', 'Great intro!', NOW() - INTERVAL '9 days'),
((SELECT id FROM Articles WHERE title = 'The Future of Web Development'), 'Alex Innovator', 'alex@innovator.com', 'Exciting!', NOW() - INTERVAL '6 days'),
((SELECT id FROM Articles WHERE title = 'Python for Data Science'), 'Data Enthusiast', 'data@enthusiast.com', 'Nice overview!', NOW() - INTERVAL '4 days');

-- 9. Create Indexes
CREATE INDEX idx_articles_author_id ON Articles(author_id);
CREATE INDEX idx_articles_publish_date ON Articles(publish_date);
CREATE INDEX idx_comments_article_id ON Comments(article_id);

-- 10. Create Views
CREATE VIEW PublishedArticles AS
SELECT
    a.id,
    a.title,
    LEFT(a.content, 200) AS excerpt,
    a.publish_date,
    au.username AS author_name,
    au.email AS author_email
FROM Articles a
JOIN Authors au ON a.author_id = au.id
WHERE a.is_published = TRUE;

CREATE VIEW ArticleCommentCounts AS
SELECT
    a.id AS article_id,
    a.title,
    COUNT(c.id) AS comment_count,
    MAX(c.comment_date) AS last_comment_date
FROM Articles a
LEFT JOIN Comments c ON a.id = c.article_id
WHERE a.is_published = TRUE
GROUP BY a.id, a.title;

-- 11. Summary Statistics
SELECT
    'Database Initialized' AS message,
    (SELECT COUNT(*) FROM Authors) AS total_authors,
    (SELECT COUNT(*) FROM Articles) AS total_articles,
    (SELECT COUNT(*) FROM Comments) AS total_comments;
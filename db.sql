-- Create database
CREATE DATABASE IF NOT EXISTS blog_db;
USE blog_db;

-- Create Authors table
CREATE TABLE Authors (
    id CHAR(36) PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create Articles table
CREATE TABLE Articles (
    id CHAR(36) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    publish_date DATETIME,
    is_published BOOLEAN DEFAULT FALSE,
    author_id CHAR(36),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (author_id) REFERENCES Authors(id) ON DELETE CASCADE
);

-- Create Comments table
CREATE TABLE Comments (
    id CHAR(36) PRIMARY KEY,
    article_id CHAR(36),
    author_name VARCHAR(255),
    author_email VARCHAR(255),
    content TEXT,
    comment_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (article_id) REFERENCES Articles(id) ON DELETE CASCADE
);

-- Insert test data into Authors
INSERT INTO Authors (id, username, email) VALUES
(UUID(), 'john_doe', 'john@example.com'),
(UUID(), 'jane_smith', 'jane@example.com'),
(UUID(), 'bob_writer', 'bob@example.com'),
(UUID(), 'alice_author', 'alice@example.com'),
(UUID(), 'charlie_blogger', 'charlie@example.com'),
(UUID(), 'diana_pen', 'diana@example.com'),
(UUID(), 'evan_words', 'evan@example.com'),
(UUID(), 'fiona_scribe', 'fiona@example.com');

-- Insert test data into Articles
INSERT INTO Articles (id, title, content, publish_date, is_published, author_id) VALUES
-- Articles by john_doe
(
    UUID(),
    'Getting Started with SQL',
    'SQL (Structured Query Language) is a standard language for managing and manipulating databases. In this article, we will cover the basics of SQL including SELECT, INSERT, UPDATE, and DELETE statements.',
    DATE_SUB(NOW(), INTERVAL 10 DAY),
    TRUE,
    (SELECT id FROM Authors WHERE username = 'john_doe')
),
(
    UUID(),
    'Database Design Best Practices',
    'Designing a database requires careful planning. This article discusses normalization, indexing, and relationship design to create efficient and scalable databases.',
    DATE_SUB(NOW(), INTERVAL 8 DAY),
    TRUE,
    (SELECT id FROM Authors WHERE username = 'john_doe')
),
-- Articles by jane_smith
(
    UUID(),
    'The Future of Web Development',
    'Web development is constantly evolving. This article explores emerging trends like serverless architecture, Jamstack, and WebAssembly.',
    DATE_SUB(NOW(), INTERVAL 7 DAY),
    TRUE,
    (SELECT id FROM Authors WHERE username = 'jane_smith')
),
(
    UUID(),
    'Introduction to React Hooks',
    'React Hooks revolutionized functional components in React. Learn how to use useState, useEffect, and custom hooks in your applications.',
    DATE_SUB(NOW(), INTERVAL 5 DAY),
    TRUE,
    (SELECT id FROM Authors WHERE username = 'jane_smith')
),
-- Articles by bob_writer
(
    UUID(),
    'Python for Data Science',
    'Python has become the go-to language for data science. This article covers essential libraries like Pandas, NumPy, and Matplotlib.',
    DATE_SUB(NOW(), INTERVAL 6 DAY),
    TRUE,
    (SELECT id FROM Authors WHERE username = 'bob_writer')
),
(
    UUID(),
    'Machine Learning Basics',
    'An introduction to machine learning concepts including supervised vs unsupervised learning, regression, and classification algorithms.',
    DATE_SUB(NOW(), INTERVAL 3 DAY),
    TRUE,
    (SELECT id FROM Authors WHERE username = 'bob_writer')
),
-- Articles by alice_author
(
    UUID(),
    'Building RESTful APIs',
    'Learn how to design and build RESTful APIs using best practices for endpoints, HTTP methods, and status codes.',
    DATE_SUB(NOW(), INTERVAL 4 DAY),
    TRUE,
    (SELECT id FROM Authors WHERE username = 'alice_author')
),
-- Draft articles
(
    UUID(),
    'Advanced Database Optimization',
    'This article is still in progress and covers advanced optimization techniques for large-scale databases.',
    NULL,
    FALSE,
    (SELECT id FROM Authors WHERE username = 'john_doe')
),
(
    UUID(),
    'Upcoming JavaScript Features',
    'A preview of upcoming ECMAScript features and how they will change JavaScript development.',
    NULL,
    FALSE,
    (SELECT id FROM Authors WHERE username = 'jane_smith')
);

-- Insert test data into Comments
INSERT INTO Comments (id, article_id, author_name, author_email, content, comment_date) VALUES
-- Comments for "Getting Started with SQL"
(
    UUID(),
    (SELECT id FROM Articles WHERE title = 'Getting Started with SQL'),
    'Mike Learner',
    'mike@learner.com',
    'Great introduction! This really helped me understand the basics.',
    DATE_SUB(NOW(), INTERVAL 9 DAY)
),
(
    UUID(),
    (SELECT id FROM Articles WHERE title = 'Getting Started with SQL'),
    'Sarah Developer',
    'sarah@dev.com',
    'Could you write a follow-up about JOIN operations?',
    DATE_SUB(NOW(), INTERVAL 8 DAY)
),
(
    UUID(),
    (SELECT id FROM Articles WHERE title = 'Getting Started with SQL'),
    'Tom Coder',
    'tom@coder.com',
    'Clear and concise explanations. Perfect for beginners!',
    DATE_SUB(NOW(), INTERVAL 7 DAY)
),
-- Comments for "The Future of Web Development"
(
    UUID(),
    (SELECT id FROM Articles WHERE title = 'The Future of Web Development'),
    'Alex Innovator',
    'alex@innovator.com',
    'Exciting trends! I am particularly interested in serverless architecture.',
    DATE_SUB(NOW(), INTERVAL 6 DAY)
),
(
    UUID(),
    (SELECT id FROM Articles WHERE title = 'The Future of Web Development'),
    'Rachel Techie',
    'rachel@techie.com',
    'WebAssembly is definitely changing the game. Great insights!',
    DATE_SUB(NOW(), INTERVAL 5 DAY)
),
-- Comments for "Python for Data Science"
(
    UUID(),
    (SELECT id FROM Articles WHERE title = 'Python for Data Science'),
    'Data Enthusiast',
    'data@enthusiast.com',
    'Pandas is a game-changer for data manipulation. Nice overview!',
    DATE_SUB(NOW(), INTERVAL 4 DAY)
),
-- Comments for "Introduction to React Hooks"
(
    UUID(),
    (SELECT id FROM Articles WHERE title = 'Introduction to React Hooks'),
    'React Fan',
    'react@fan.com',
    'Hooks made my React code so much cleaner. Thanks for the tutorial!',
    DATE_SUB(NOW(), INTERVAL 3 DAY)
),
(
    UUID(),
    (SELECT id FROM Articles WHERE title = 'Introduction to React Hooks'),
    'Junior Developer',
    'junior@dev.com',
    'I struggled with hooks before reading this. Now it makes sense!',
    DATE_SUB(NOW(), INTERVAL 2 DAY)
),
-- Comments for "Machine Learning Basics"
(
    UUID(),
    (SELECT id FROM Articles WHERE title = 'Machine Learning Basics'),
    'ML Beginner',
    'ml@beginner.com',
    'Finally found an ML introduction that doesnt require a PhD!',
    DATE_SUB(NOW(), INTERVAL 1 DAY)
),
(
    UUID(),
    (SELECT id FROM Articles WHERE title = 'Machine Learning Basics'),
    'AI Researcher',
    'ai@researcher.com',
    'Good overview of fundamental concepts. Well structured.',
    NOW()
);

-- Create indexes for better performance
CREATE INDEX idx_articles_author_id ON Articles(author_id);
CREATE INDEX idx_articles_publish_date ON Articles(publish_date);
CREATE INDEX idx_comments_article_id ON Comments(article_id);
CREATE INDEX idx_comments_comment_date ON Comments(comment_date);

-- Create a view for published articles with author info
CREATE VIEW PublishedArticles AS
SELECT 
    a.id,
    a.title,
    SUBSTRING(a.content, 1, 200) AS excerpt,
    a.publish_date,
    au.username AS author_name,
    au.email AS author_email
FROM Articles a
JOIN Authors au ON a.author_id = au.id
WHERE a.is_published = TRUE
ORDER BY a.publish_date DESC;

-- Create a view for article comment counts
CREATE VIEW ArticleCommentCounts AS
SELECT 
    a.id AS article_id,
    a.title,
    COUNT(c.id) AS comment_count,
    MAX(c.comment_date) AS last_comment_date
FROM Articles a
LEFT JOIN Comments c ON a.id = c.article_id
WHERE a.is_published = TRUE
GROUP BY a.id, a.title
ORDER BY comment_count DESC;

-- Display summary statistics
SELECT 
    'Database Created Successfully' AS message,
    (SELECT COUNT(*) FROM Authors) AS total_authors,
    (SELECT COUNT(*) FROM Articles) AS total_articles,
    (SELECT COUNT(*) FROM Articles WHERE is_published = TRUE) AS published_articles,
    (SELECT COUNT(*) FROM Articles WHERE is_published = FALSE) AS draft_articles,
    (SELECT COUNT(*) FROM Comments) AS total_comments;
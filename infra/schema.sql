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
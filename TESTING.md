# Blog Application Testing Guide

This guide provides practical examples for testing the Blog Application across all three exposure modes: REST API, MCP stdio, and MCP HTTP.

## Table of Contents

1. [Database Setup](#database-setup)
2. [REST API Testing](#rest-api-testing)
3. [MCP HTTP Testing](#mcp-http-testing)
4. [MCP stdio Testing](#mcp-stdio-testing)
5. [Testing with MCP Inspector](#testing-with-mcp-inspector)
6. [Testing with Claude Desktop](#testing-with-claude-desktop)
7. [Troubleshooting](#troubleshooting)

---

## Database Setup

The Blog Application supports multiple database options. Choose the setup that best fits your testing needs.

### Option 1: H2 In-Memory Database (Default - Quickest)

The application uses H2 in-memory database by default for rapid testing and development.

**Advantages**:
- ✅ No external database required
- ✅ Automatic initialization on startup
- ✅ Pre-populated with test data (8 authors, 9 articles, 10 comments)
- ✅ Perfect for quick testing and demos

**Usage**:

```bash
# Just run the application - H2 is used by default
java -jar target/blog-app-1.0-SNAPSHOT.jar --config blog-rest.yaml

# Data is automatically loaded from src/main/resources/schema.sql
```

**Limitations**:
- Doesn't support advanced features of Postgres, some actions will fail.
- Data is lost when the application stops
- Not suitable for persistent testing
- Limited to single application instance

**Pre-populated Test Data**:
- **8 Authors**: john_doe, jane_smith, bob_writer, alice_author, charlie_blogger, diana_pen, evan_words, fiona_scribe
- **9 Articles**: 7 published, 2 drafts across various topics
- **10 Comments**: Distributed across published articles

---

### Option 2: PostgreSQL with Docker (Recommended for Development)

Use PostgreSQL for persistent data and multi-user testing.

**Advantages**:
- ✅ Persistent data across restarts
- ✅ Production-like environment
- ✅ Easy to reset and populate with test data
- ✅ Supports concurrent connections

#### Step 1: Start PostgreSQL with Docker Compose

```bash
cd infra
docker-compose up -d
```

The `docker-compose.yml` configuration:
```yaml
services:
  chinook-db:
    image: postgres:16-alpine
    container_name: blog-db
    environment:
      POSTGRES_USER: blog_user
      POSTGRES_PASSWORD: blog_password
      POSTGRES_DB: blog_db
    ports:
      - "5432:5432"
    volumes:
      - ./postgres/init-db:/docker-entrypoint-initdb.d
      - ./blog_data:/var/lib/postgresql/data
```

**What happens on first start**:
1. PostgreSQL container starts
2. Database `blog_db` is created
3. Scripts in `postgres/init-db/` are executed automatically:
   - `02-schema-post.sql` creates tables, indexes, views, and seed data

#### Step 2: Verify Database

```bash
# Check container status
docker ps | grep blog-db

# Connect to database
docker exec -it blog-db psql -U blog_user -d blog_db

# Inside psql, verify tables
\dt

# Check data
SELECT COUNT(*) FROM Authors;
SELECT COUNT(*) FROM Articles;
SELECT COUNT(*) FROM Comments;

# Exit psql
\q
```

#### Step 3: Configure Application for PostgreSQL

Update `blog-application.yaml` datasource configuration:

```yaml
datasources:
  blog_db:
    type: postgres
    config:
      url: jdbc:postgresql://localhost:5432/blog_db
      username: blog_user
      password: blog_password
      driverClassName: org.postgresql.Driver
      maxPoolSize: 10
```

#### Step 4: Run Application with PostgreSQL

```bash
java -jar target/blog-app-1.0-SNAPSHOT.jar --config blog-rest.yaml
```

---

### Option 3: Generate Custom Test Data

For performance testing or custom scenarios, generate realistic test data using Python scripts.

#### Prerequisites

Install Python dependencies:

```bash
cd infra/postgres

# Option 1: Use existing virtual environment
source venv/bin/activate  # Linux/Mac
# or
venv\Scripts\activate     # Windows

# Option 2: Install dependencies globally
pip install -r requirements.txt
```

Required packages:
- `faker` - Generate realistic test data
- `psycopg2-binary` - PostgreSQL connectivity

#### Generate SQL File

**Small Dataset (Quick Testing)**:
```bash
python generate_test_data.py \
  --authors 20 \
  --articles 50 \
  --comments 200 \
  --output small_data.sql
```

**Medium Dataset (Development)**:
```bash
python generate_test_data.py \
  --authors 100 \
  --articles 500 \
  --comments 2000 \
  --output medium_data.sql
```

**Large Dataset (Performance Testing)**:
```bash
python generate_test_data.py \
  --authors 500 \
  --articles 5000 \
  --comments 25000 \
  --output large_data.sql
```

**Very Large Dataset (Stress Testing)**:
```bash
python generate_test_data.py \
  --authors 1000 \
  --articles 10000 \
  --comments 50000 \
  --output very_large_data.sql
```

**Advanced Options**:

```bash
# Reproducible data with seed
python generate_test_data.py --seed 42 --output data.sql

# Control published vs draft ratio
python generate_test_data.py --published-ratio 0.8 --output data.sql  # 80% published

# Custom amounts
python generate_test_data.py \
  --authors 200 \
  --articles 1000 \
  --comments 5000 \
  --published-ratio 0.7 \
  --seed 123 \
  --output custom_data.sql
```

#### Populate Database with Generated Data

**Method 1: Python Script (Recommended)**

The `populate_db.py` script provides automatic population with progress tracking:

```bash
cd infra/postgres
source venv/bin/activate

# Use existing SQL file
python populate_db.py --sql-file small_data.sql

# Generate and populate in one step
python populate_db.py --generate --authors 100 --articles 500 --comments 2000

# Custom connection
python populate_db.py \
  --sql-file medium_data.sql \
  --host localhost \
  --port 5432 \
  --user blog_user \
  --password blog_password \
  --database blog_db
```

The script will:
- ✅ Connect to database with retry logic
- ✅ Show progress during execution
- ✅ Display table counts before and after
- ✅ Handle errors gracefully

**Method 2: Using psql**

```bash
# Direct psql
psql -U blog_user -d blog_db -h localhost -f small_data.sql

# Using Docker
docker exec -i blog-db psql -U blog_user -d blog_db < small_data.sql
```

**Method 3: Using Docker Copy & Exec**

```bash
# Copy SQL file to container
docker cp small_data.sql blog-db:/tmp/data.sql

# Execute in container
docker exec blog-db psql -U blog_user -d blog_db -f /tmp/data.sql
```

#### Verify Population

```bash
# Using psql
docker exec -it blog-db psql -U blog_user -d blog_db

# Inside psql
SELECT 'Authors' AS table_name, COUNT(*) AS count FROM Authors
UNION ALL
SELECT 'Articles', COUNT(*) FROM Articles
UNION ALL
SELECT 'Comments', COUNT(*) FROM Comments;

# Check published vs draft articles
SELECT 
  is_published,
  COUNT(*) AS count 
FROM Articles 
GROUP BY is_published;

# View sample data
SELECT 
  a.username,
  COUNT(ar.id) AS article_count,
  COUNT(CASE WHEN ar.is_published THEN 1 END) AS published_count
FROM Authors a
LEFT JOIN Articles ar ON a.id = ar.author_id
GROUP BY a.username
LIMIT 10;
```

---

### Option 4: Manual PostgreSQL Setup (Without Docker)

If you prefer a local PostgreSQL installation:

#### Step 1: Install PostgreSQL

**Ubuntu/Debian**:
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
```

**macOS**:
```bash
brew install postgresql@16
brew services start postgresql@16
```

**Windows**: Download installer from [postgresql.org](https://www.postgresql.org/download/windows/)

#### Step 2: Create Database and User

```bash
# Connect as postgres user
sudo -u postgres psql

# Inside psql
CREATE DATABASE blog_db;
CREATE USER blog_user WITH PASSWORD 'blog_password';
GRANT ALL PRIVILEGES ON DATABASE blog_db TO blog_user;

# Grant schema permissions (PostgreSQL 15+)
\c blog_db
GRANT ALL ON SCHEMA public TO blog_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO blog_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO blog_user;

\q
```

#### Step 3: Run Schema Script

```bash
# Run the initialization script
psql -U blog_user -d blog_db -f infra/postgres/init-db/02-schema-post.sql
```

#### Step 4: Update Application Configuration

Edit `blog-application.yaml`:

```yaml
datasources:
  blog_db:
    type: postgres
    config:
      url: jdbc:postgresql://localhost:5432/blog_db
      username: blog_user
      password: blog_password
```

---

### Database Management Commands

#### Reset Database (PostgreSQL)

**Using Docker**:
```bash
# Stop and remove container with data
cd infra
docker-compose down -v

# Start fresh
docker-compose up -d
```

**Manual Reset**:
```bash
# Connect to database
docker exec -it blog-db psql -U blog_user -d blog_db

# Drop and recreate schema
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;

# Re-run initialization
\i /docker-entrypoint-initdb.d/02-schema-post.sql

\q
```

#### Backup and Restore

**Backup**:
```bash
# Using Docker
docker exec blog-db pg_dump -U blog_user blog_db > backup.sql

# Direct
pg_dump -U blog_user -h localhost -d blog_db > backup.sql
```

**Restore**:
```bash
# Using Docker
docker exec -i blog-db psql -U blog_user -d blog_db < backup.sql

# Direct
psql -U blog_user -h localhost -d blog_db < backup.sql
```

#### View Database Statistics

```bash
docker exec -it blog-db psql -U blog_user -d blog_db
```

```sql
-- Summary view
SELECT
    'Authors' AS table_name,
    COUNT(*) AS total_rows,
    pg_size_pretty(pg_total_relation_size('Authors')) AS size
FROM Authors
UNION ALL
SELECT 'Articles', COUNT(*), pg_size_pretty(pg_total_relation_size('Articles'))
FROM Articles
UNION ALL
SELECT 'Comments', COUNT(*), pg_size_pretty(pg_total_relation_size('Comments'))
FROM Comments;

-- Most active authors
SELECT 
    a.username,
    a.email,
    COUNT(DISTINCT ar.id) AS total_articles,
    COUNT(DISTINCT CASE WHEN ar.is_published THEN ar.id END) AS published_articles,
    COUNT(DISTINCT c.id) AS total_comments_on_articles
FROM Authors a
LEFT JOIN Articles ar ON a.id = ar.author_id
LEFT JOIN Comments c ON ar.id = c.article_id
GROUP BY a.id, a.username, a.email
ORDER BY total_articles DESC
LIMIT 10;

-- Articles with most comments
SELECT 
    a.title,
    au.username AS author,
    COUNT(c.id) AS comment_count,
    a.publish_date
FROM Articles a
JOIN Authors au ON a.author_id = au.id
LEFT JOIN Comments c ON a.id = c.article_id
WHERE a.is_published = TRUE
GROUP BY a.id, a.title, au.username, a.publish_date
ORDER BY comment_count DESC
LIMIT 10;
```

---

### Database Comparison

| Feature | H2 In-Memory | PostgreSQL (Docker) | PostgreSQL (Manual) |
|---------|--------------|---------------------|---------------------|
| **Setup Time** | Instant | 2-3 minutes | 10-15 minutes |
| **Persistence** | ❌ No | ✅ Yes | ✅ Yes |
| **Test Data** | 8/9/10 (built-in) | Customizable | Customizable |
| **Performance** | Fast | Very Fast | Very Fast |
| **Multi-Connection** | Limited | ✅ Yes | ✅ Yes |
| **Production-Like** | ❌ No | ✅ Yes | ✅ Yes |
| **Best For** | Quick demos, unit tests | Development, integration tests | CI/CD, production testing |

---

### Quick Start Recommendations

**For Quick Demo**:
```bash
# Use H2 - just run the app
java -jar target/blog-app-1.0-SNAPSHOT.jar --config blog-rest.yaml
```

**For Development**:
```bash
# Use PostgreSQL with Docker
cd infra
docker-compose up -d
# Wait 10 seconds for initialization
java -jar target/blog-app-1.0-SNAPSHOT.jar --config blog-rest.yaml
```

**For Performance Testing**:
```bash
# PostgreSQL with large dataset
cd infra
docker-compose up -d
cd postgres
source venv/bin/activate
python populate_db.py --generate --authors 500 --articles 5000 --comments 25000
cd ../..
java -jar target/blog-app-1.0-SNAPSHOT.jar --config blog-rest.yaml
```

---

## REST API Testing

### Prerequisites

Before testing, ensure you have a database set up. See [Database Setup](#database-setup) section for options:
- **Quick Start**: Use H2 in-memory database (default - no setup needed)
- **Development**: Use PostgreSQL with Docker for persistent data

### Start the Application

Start the application in REST mode:

```bash
java -jar target/blog-app-1.0-SNAPSHOT.jar --config blog-rest.yaml
```

The REST API is available at `http://localhost:9000/api/v1/blog`

### Authors Operations

#### Create Author

```bash
curl -v "http://localhost:9000/api/v1/blog/create_author?username=john_doe&email=john@example.com"
```

Expected response:
```json
{
  "success": true,
  "data": [{
    "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "username": "john_doe",
    "email": "john@example.com",
    "created_at": "2026-01-12T10:30:00Z"
  }],
  "meta": {
    "operation": "create_author",
    "timestamp": "2026-01-12T10:30:00Z"
  }
}
```

#### List Authors with Pagination

```bash
curl "http://localhost:9000/api/v1/blog/list_authors?page=1&limit=10"
```

#### Get Author Details with Statistics

```bash
# Replace <author-id> with an actual UUID from list_authors response
curl "http://localhost:9000/api/v1/blog/author_details?id=<author-id>"
```

Expected response includes author info plus statistics:
```json
{
  "success": true,
  "data": [{
    "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "username": "john_doe",
    "email": "john@example.com",
    "created_at": "2026-01-12T10:30:00Z",
    "total_articles": 5,
    "published_articles": 3,
    "total_comments": 12
  }]
}
```

#### Update Author

```bash
curl "http://localhost:9000/api/v1/blog/update_author?id=<author-id>&email=newemail@example.com"
```

#### Delete Author

```bash
curl "http://localhost:9000/api/v1/blog/delete_author?id=<author-id>"
```

### Articles Operations

#### Create Article

```bash
curl "http://localhost:9000/api/v1/blog/create_article?author_id=<author-id>&title=My%20First%20Post&content=This%20is%20the%20article%20content&is_published=true"
```

#### List Articles by Author

```bash
curl "http://localhost:9000/api/v1/blog/list_articles_by_author?author_id=<author-id>&published=true&limit=20&page=1"
```

#### Update Article

```bash
curl "http://localhost:9000/api/v1/blog/update_article?id=<article-id>&title=Updated%20Title&is_published=true"
```

### Comments Operations

#### Add Comment to Article

```bash
curl "http://localhost:9000/api/v1/blog/comment_on_article?article_id=<article-id>&author_name=Jane&author_email=jane@example.com&content=Great%20article!"
```

#### List Comments for Article

```bash
curl "http://localhost:9000/api/v1/blog/list_comments_by_article?article_id=<article-id>&limit=50&page=1"
```

#### Update Comment

```bash
curl "http://localhost:9000/api/v1/blog/update_comment?id=<comment-id>&content=Updated%20comment%20text"
```

### Statistics Operations

#### Get System Overview

```bash
curl "http://localhost:9000/api/v1/blog/stats_overview"
```

Expected response:
```json
{
  "success": true,
  "data": [{
    "total_authors": 8,
    "total_articles": 15,
    "published_articles": 12,
    "draft_articles": 3,
    "total_comments": 45,
    "avg_articles_per_author": 1.875,
    "avg_comments_per_article": 3.0,
    "most_active_author": "john_doe",
    "most_commented_article": "Introduction to Functional Programming"
  }]
}
```

#### Get Statistics for Timeframe

```bash
# Timeframe options: day, week, month, year
curl "http://localhost:9000/api/v1/blog/stats_overview?timeframe=month"
```

---

## MCP HTTP Testing

Start the application in MCP HTTP mode:

```bash
java -jar target/blog-app-1.0-SNAPSHOT.jar --config blog-mcp-streamable-http.yaml
```

The MCP endpoint is available at `http://localhost:9000/mcp/v1/`

### Step 1: Initialize MCP Session

```bash
curl -v http://localhost:9000/mcp/v1/ \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream,application/json" \
  -d '{
    "jsonrpc": "2.0", 
    "id": 1, 
    "method": "initialize", 
    "params": {
      "protocolVersion": "2024-11-05",
      "capabilities": {},
      "clientInfo": {"name": "curl-test", "version": "1.0"}
    }
  }'
```

**Important**: Save the `Mcp-Session-Id` from the response headers. You'll need it for all subsequent requests.

Example response:
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "protocolVersion": "2024-11-05",
    "capabilities": {
      "logging": {},
      "resources": {
        "subscribe": true,
        "listChanged": true
      },
      "tools": {
        "listChanged": true
      },
      "prompts": {}
    },
    "serverInfo": {
      "name": "blog-mcp-server",
      "version": "1.0.0"
    }
  }
}
```

Response headers will include:
```
Mcp-Session-Id: a1b2c3d4-e5f6-7890-abcd-ef1234567890
```

### Step 2: Complete Handshake

```bash
curl -v http://localhost:9000/mcp/v1/ \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream,application/json" \
  -H "Mcp-Session-Id: bf3633b2-ef03-4efc-bcca-5b2e1ea9a91f" \
  -d '{
    "jsonrpc": "2.0",
    "method": "notifications/initialized"
  }'
```

Expected response: `HTTP/1.1 202 Accepted`

### Step 3: List Available Tools

```bash
curl -v http://localhost:9000/mcp/v1/ \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream,application/json" \
  -H "Mcp-Session-Id: bf3633b2-ef03-4efc-bcca-5b2e1ea9a91f" \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/list"
  }'
```

Expected response (partial):
```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "result": {
    "tools": [
      {
        "name": "create_author",
        "description": "Create a new blog author with username and email",
        "inputSchema": {
          "type": "object",
          "properties": {
            "username": {
              "type": "string",
              "description": "Unique username (3-255 chars, alphanumeric + underscore)"
            },
            "email": {
              "type": "string",
              "description": "Valid email address"
            }
          },
          "required": ["username", "email"]
        }
      },
      {
        "name": "list_authors",
        "description": "Retrieve paginated list of authors with optional filtering and statistics"
      },
      ...
    ]
  }
}
```

### Step 4: Call a Tool - Create Author

```bash
curl -v -N -X POST http://localhost:9000/mcp/v1/ \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream,application/json" \
  -H "Mcp-Session-Id: bf3633b2-ef03-4efc-bcca-5b2e1ea9a91f" \
  -d '{
    "jsonrpc": "2.0",
    "id": 3,
    "method": "tools/call",
    "params": {
      "name": "create_author",
      "arguments": {
        "username": "alice_writer_3",
        "email": "alice_3@example.com"
      }
    }
  }'
```

### Step 5: Call More Tools

#### Create Article

```bash
curl -v -N -X POST http://localhost:9000/mcp/v1/ \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream,application/json" \
  -H "Mcp-Session-Id: a1b2c3d4-e5f6-7890-abcd-ef1234567890" \
  -d '{
    "jsonrpc": "2.0",
    "id": 4,
    "method": "tools/call",
    "params": {
      "name": "create_article",
      "arguments": {
        "author_id": "<author-id>",
        "title": "Introduction to MCP",
        "content": "Model Context Protocol enables seamless AI integration...",
        "is_published": true
      }
    }
  }'
```

#### List Articles by Author

```bash
curl -v -N -X POST http://localhost:9000/mcp/v1/ \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream,application/json" \
  -H "Mcp-Session-Id: a1b2c3d4-e5f6-7890-abcd-ef1234567890" \
  -d '{
    "jsonrpc": "2.0",
    "id": 5,
    "method": "tools/call",
    "params": {
      "name": "list_articles_by_author",
      "arguments": {
        "author_id": "<author-id>",
        "published": true,
        "limit": 10,
        "page": 1
      }
    }
  }'
```

#### Add Comment

```bash
curl -v -N -X POST http://localhost:9000/mcp/v1/ \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream,application/json" \
  -H "Mcp-Session-Id: a1b2c3d4-e5f6-7890-abcd-ef1234567890" \
  -d '{
    "jsonrpc": "2.0",
    "id": 6,
    "method": "tools/call",
    "params": {
      "name": "comment_on_article",
      "arguments": {
        "article_id": "<article-id>",
        "author_name": "Bob Reader",
        "author_email": "bob@example.com",
        "content": "Excellent explanation!"
      }
    }
  }'
```

#### Get Statistics

```bash
curl -v -N -X POST http://localhost:9000/mcp/v1/ \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream,application/json" \
  -H "Mcp-Session-Id: a1b2c3d4-e5f6-7890-abcd-ef1234567890" \
  -d '{
    "jsonrpc": "2.0",
    "id": 7,
    "method": "tools/call",
    "params": {
      "name": "stats_overview",
      "arguments": {
        "timeframe": "month"
      }
    }
  }'
```

### Step 6: List and Read Resources

#### List Available Resources

```bash
curl -v http://localhost:9000/mcp/v1/ \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream,application/json" \
  -H "Mcp-Session-Id: a1b2c3d4-e5f6-7890-abcd-ef1234567890" \
  -d '{
    "jsonrpc": "2.0",
    "id": 8,
    "method": "resources/list"
  }'
```

#### Read OpenAPI Specification

```bash
curl -v -N -X POST http://localhost:9000/mcp/v1/ \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream,application/json" \
  -H "Mcp-Session-Id: a1b2c3d4-e5f6-7890-abcd-ef1234567890" \
  -d '{
    "jsonrpc": "2.0",
    "id": 9,
    "method": "resources/read",
    "params": {
      "uri": "docs://openapi_spec"
    }
  }'
```

#### Read Database Schema

```bash
curl -v -N -X POST http://localhost:9000/mcp/v1/ \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream,application/json" \
  -H "Mcp-Session-Id: a1b2c3d4-e5f6-7890-abcd-ef1234567890" \
  -d '{
    "jsonrpc": "2.0",
    "id": 10,
    "method": "resources/read",
    "params": {
      "uri": "docs://database_schema"
    }
  }'
```

### Step 7: List and Use Prompts

#### List Available Prompts

```bash
curl -v http://localhost:9000/mcp/v1/ \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream,application/json" \
  -H "Mcp-Session-Id: a1b2c3d4-e5f6-7890-abcd-ef1234567890" \
  -d '{
    "jsonrpc": "2.0",
    "id": 11,
    "method": "prompts/list"
  }'
```

---

## MCP stdio Testing

Start the application in MCP stdio mode:

```bash
java -jar target/blog-app-1.0-SNAPSHOT.jar \
  --config blog-mcp-stdio.yaml \
  --log-file /tmp/blog-mcp-stdio.log \
  --redirect-stderr
```

**⚠️ CRITICAL**: The `--redirect-stderr` flag is **REQUIRED** to prevent log messages from corrupting the JSON-RPC communication on stdout.

### Complete MCP stdio Workflow

Type or paste these JSON-RPC messages into stdin (one per line):

#### 1. Initialize

```json
{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test-client","version":"1.0.0"}}}
```

#### 2. Complete Handshake

```json
{"jsonrpc":"2.0","method":"notifications/initialized","params":{}}
```

#### 3. List Tools

```json
{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}
```

#### 4. List Resources

```json
{"jsonrpc":"2.0","id":3,"method":"resources/list","params":{}}
```

#### 5. List Prompts

```json
{"jsonrpc":"2.0","id":4,"method":"prompts/list","params":{}}
```

#### 6. Create Author

```json
{"jsonrpc":"2.0","id":5,"method":"tools/call","params":{"name":"create_author","arguments":{"username":"test_user","email":"test@example.com"}}}
```

#### 7. List Authors

```json
{"jsonrpc":"2.0","id":6,"method":"tools/call","params":{"name":"list_authors","arguments":{"page":1,"limit":10}}}
```

#### 8. Get Author Details

```json
{"jsonrpc":"2.0","id":7,"method":"tools/call","params":{"name":"author_details","arguments":{"id":"<author-id>"}}}
```

#### 9. Create Article

```json
{"jsonrpc":"2.0","id":8,"method":"tools/call","params":{"name":"create_article","arguments":{"author_id":"<author-id>","title":"Test Article","content":"Article content here","is_published":true}}}
```

#### 10. List Articles by Author

```json
{"jsonrpc":"2.0","id":9,"method":"tools/call","params":{"name":"list_articles_by_author","arguments":{"author_id":"<author-id>","limit":20,"page":1}}}
```

#### 11. Comment on Article

```json
{"jsonrpc":"2.0","id":10,"method":"tools/call","params":{"name":"comment_on_article","arguments":{"article_id":"<article-id>","author_name":"Commenter","author_email":"commenter@example.com","content":"Great post!"}}}
```

#### 12. Get Statistics

```json
{"jsonrpc":"2.0","id":11,"method":"tools/call","params":{"name":"stats_overview","arguments":{}}}
```

#### 13. Read API Documentation

```json
{"jsonrpc":"2.0","id":12,"method":"tools/call","params":{"name":"get_api_documentation","arguments":{}}}
```

#### 14. Read OpenAPI Spec

```json
{"jsonrpc":"2.0","id":13,"method":"tools/call","params":{"name":"get_openapi_spec","arguments":{}}}
```

---

## Testing with MCP Inspector

The MCP Inspector provides a visual web interface for testing MCP servers.

### Install MCP Inspector

```bash
npm install -g @modelcontextprotocol/inspector
```

### Run Blog App with Inspector

#### Using the provided script:

```bash
cd idea-projects/cheshire-framework/cheshire-blog-app
bash dev-utils/blog-inspector.sh
```

#### Or manually:

```bash
npx @modelcontextprotocol/inspector -- java -jar target/blog-app-1.0-SNAPSHOT.jar \
  --config blog-mcp-stdio.yaml \
  --log-file /tmp/blog-mcp-stdio.log \
  --redirect-stderr
```

### Access Inspector UI

Open your browser to: `http://localhost:5173`

The inspector provides:
- **Visual tool browser**: See all 15+ available tools
- **Interactive tool testing**: Fill forms instead of writing JSON
- **Request/response inspection**: Debug protocol messages
- **Resource explorer**: Browse schemas, documentation, and data
- **Prompt templates**: Test AI-assisted workflows

### Inspector Workflow Example

1. Open `http://localhost:5173`
2. Click **"Initialize"** to start the MCP session
3. Navigate to **"Tools"** tab
4. Select **"create_author"** from the list
5. Fill in the form:
   - Username: `inspector_user`
   - Email: `inspector@example.com`
6. Click **"Call Tool"**
7. View the response in the results panel
8. Repeat with other tools like `list_authors`, `create_article`, etc.

---

## Testing with Claude Desktop

### Configuration for Claude Desktop

#### Linux/Mac Configuration

Edit `~/.config/Claude/claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "blog-api": {
      "command": "java",
      "args": [
        "-jar",
        "/path/to/blog-app/target/blog-app-1.0-SNAPSHOT.jar",
        "--config",
        "blog-mcp-stdio.yaml",
        "--log-file",
        "/tmp/blog-mcp-stdio.log",
        "--redirect-stderr"
      ],
      "env": {
        "LOG_LEVEL": "INFO"
      }
    }
  }
}
```

#### Windows with WSL2

Edit `%APPDATA%\Claude\claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "blog-api": {
      "command": "wsl",
      "args": [
        "bash",
        "[REPLACE_WITH_YOURLOCAL_PATH]/cheshire-blog-app/dev-utils/blog-app-claude.sh"
      ]
    }
  }
}
```

### Make Script Executable (WSL2)

```bash
chmod +x [REPLACE_WITH_YOURLOCAL_PATH]/cheshire-blog-app/dev-utils/blog-app-claude.sh
```

### Test with Claude

Restart Claude Desktop and try these prompts:

#### Author Management

```
Using the blog API, create a new author named Sarah Johnson with email sarah@example.com
```

```
Show me a list of all authors in the blog system
```

```
Get detailed statistics for author with ID <author-id>
```

#### Article Creation

```
Create a new article titled "Getting Started with MCP" by author <author-id> with some sample content and publish it
```

```
List all published articles by author <author-id>
```

#### Comments

```
Add a comment to article <article-id> from "John Reader" saying "This is very helpful, thank you!"
```

```
Show me all comments for article <article-id>
```

#### Statistics

```
Give me an overview of the blog system statistics
```

```
Show me statistics for the last month
```

#### Documentation Access

```
Show me the OpenAPI specification for the blog API
```

```
Display the database schema
```

### Viewing Claude Logs

**Windows**: `%USERPROFILE%\AppData\Roaming\Claude\logs\`

**Linux/Mac**: `~/.config/Claude/logs/`

**WSL2 Logs**: `/tmp/blog-mcp-stdio.log`

```bash
# Monitor logs in real-time
tail -f /tmp/blog-mcp-stdio.log

# Search for errors
grep ERROR /tmp/blog-mcp-stdio.log

# Search for specific operations
grep "create_author" /tmp/blog-mcp-stdio.log
```

---

## Troubleshooting

### Common Issues and Solutions

#### 1. MCP stdio: JSON Parse Errors

**Symptom**: Messages like `Unexpected token` or `Invalid JSON`

**Cause**: Log messages corrupting stdout

**Solution**: Always use `--redirect-stderr`:

```bash
java -jar target/blog-app-1.0-SNAPSHOT.jar \
  --config blog-mcp-stdio.yaml \
  --log-file /tmp/blog-mcp-stdio.log \
  --redirect-stderr  # ← REQUIRED!
```

**Verify**:
```bash
# Check logs are going to file, not stdout
tail -f /tmp/blog-mcp-stdio.log
```

---

#### 2. MCP HTTP: 301 Moved Permanently

**Symptom**: Curl returns 301 redirect instead of response

**Cause**: Missing trailing slash in URL

**Wrong**:
```bash
curl http://localhost:9000/mcp/v1
```

**Correct**:
```bash
curl http://localhost:9000/mcp/v1/
```

---

#### 3. MCP HTTP: 400 Bad Request - Accept Header Required

**Symptom**: Error message: `text/event-stream required in Accept header`

**Cause**: MCP HTTP transport requires specific Accept header

**Wrong**:
```bash
curl -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}'
```

**Correct**:
```bash
curl -H "Content-Type: application/json" \
  -H "Accept: text/event-stream,application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{...}}'
```

---

#### 4. MCP HTTP: Session Not Found

**Symptom**: 404 or "Session not found" errors

**Cause**: Missing `Mcp-Session-Id` header after initialization

**Solution**: Always include the session ID from initialization response:

```bash
curl -H "Mcp-Session-Id: a1b2c3d4-e5f6-7890-abcd-ef1234567890" \
  ...
```

---

#### 5. Tools List Returns Empty

**Symptom**: `tools/list` returns empty array

**Cause**: Didn't call `notifications/initialized` after initialization

**Solution**: Always complete the handshake:

```bash
# 1. Initialize
curl ... -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{...}}'

# 2. Complete handshake (DON'T SKIP THIS!)
curl ... -d '{"jsonrpc":"2.0","method":"notifications/initialized"}'

# 3. Now list tools
curl ... -d '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}'
```

---

#### 6. Port Already in Use

**Symptom**: `Address already in use` error on startup

**Solution**: Change port in `blog-application.yaml`:

```yaml
transports:
  jetty:
    config:
      port: 9001  # Change to available port
```

Or kill the process using the port:
```bash
# Find process
lsof -i :9000

# Kill process
kill -9 <PID>
```

---

#### 7. WSL2: Java Not Found

**Symptom**: `java: command not found` when running from Windows

**Solution**: Install Java 21 in WSL2:

```bash
wsl sudo apt update
wsl sudo apt install openjdk-21-jdk
wsl java -version  # Verify
```

---

#### 8. WSL2: Script Permission Denied

**Symptom**: `Permission denied` when executing shell script

**Solution**: Make script executable:

```bash
chmod +x [REPLACE_WITH_YOURLOCAL_PATH]/cheshire-blog-app/dev-utils/blog-app-claude.sh
```

---

#### 9. Claude Desktop: Server Won't Start

**Symptom**: Claude shows "Server connection failed"

**Check**:

1. **Verify configuration path**:
   ```bash
   # Test manually
   wsl bash /path/to/blog-app-claude.sh
   ```

2. **Check logs**:
   ```bash
   tail -f /tmp/blog-mcp-stdio.log
   ```

3. **Verify JAR exists**:
   ```bash
   ls -la [REPLACE_WITH_YOURLOCAL_PATH]/cheshire-blog-app/target/blog-app-1.0-SNAPSHOT.jar
   ```

4. **Build if needed**:
   ```bash
   cd [REPLACE_WITH_YOURLOCAL_PATH]/cheshire-blog-app
   mvn clean package
   ```

---

#### 10. Inspector: Can't Connect

**Symptom**: MCP Inspector shows connection error

**Solution**:

1. **Make sure to use `--redirect-stderr`** when launching with inspector
2. **Check stderr is not polluting stdio**:
   ```bash
   # Logs should go to file, not console
   tail -f /tmp/blog-mcp-stdio.log
   ```

3. **Restart with correct flags**:
   ```bash
   npx @modelcontextprotocol/inspector -- java -jar target/blog-app-1.0-SNAPSHOT.jar \
     --config blog-mcp-stdio.yaml \
     --log-file /tmp/blog-mcp-stdio.log \
     --redirect-stderr
   ```

---

#### 11. Data Lost After Restart (H2 In-Memory)

**Symptom**: All data disappears when application restarts

**Cause**: Using H2 in-memory database (default)

**Solution**: Switch to PostgreSQL for persistent data:

1. **Start PostgreSQL**:
   ```bash
   cd infra
   docker-compose up -d
   ```

2. **Update configuration** in `blog-application.yaml`:
   ```yaml
   datasources:
     blog_db:
       type: postgres
       config:
         url: jdbc:postgresql://localhost:5432/blog_db
         username: blog_user
         password: blog_password
   ```

3. **Restart application**:
   ```bash
   java -jar target/blog-app-1.0-SNAPSHOT.jar --config blog-rest.yaml
   ```

---

#### 12. Database Connection Failed (PostgreSQL)

**Symptom**: `Connection refused` or `FATAL: database "blog_db" does not exist`

**Check**:

1. **Verify Docker container is running**:
   ```bash
   docker ps | grep blog-db
   ```

2. **Start container if stopped**:
   ```bash
   cd infra
   docker-compose up -d
   ```

3. **Check logs**:
   ```bash
   docker logs blog-db
   ```

4. **Verify database exists**:
   ```bash
   docker exec -it blog-db psql -U blog_user -l
   ```

5. **Recreate database if needed**:
   ```bash
   docker-compose down -v
   docker-compose up -d
   ```

---

#### 13. No Test Data in Database

**Symptom**: Empty tables after database setup

**Solution**:

**For H2**: Data is auto-loaded from `schema.sql` on startup

**For PostgreSQL with Docker**:
- Data should auto-load from `init-db/02-schema-post.sql`
- If not, manually run:
  ```bash
  docker exec -i blog-db psql -U blog_user -d blog_db < infra/postgres/init-db/02-schema-post.sql
  ```

**For More Test Data**:
```bash
cd infra/postgres
source venv/bin/activate
python populate_db.py --generate --authors 100 --articles 500 --comments 2000
```

---

## Testing Complete Workflow

Here's a complete end-to-end testing scenario:

### Prerequisites for All Workflows

**Database Setup**: Choose your database option from the [Database Setup](#database-setup) section:

```bash
# Option 1: Use H2 (default) - no setup needed

# Option 2: Use PostgreSQL with Docker
cd infra
docker-compose up -d
cd ..
```

### REST API Workflow

```bash
# 1. Start server (H2 default)
java -jar target/blog-app-1.0-SNAPSHOT.jar --config blog-rest.yaml

# 2. Create author
AUTHOR_ID=$(curl -s "http://localhost:9000/api/v1/blog/create_author?username=alice&email=alice@example.com" | jq -r '.data[0].id')
echo "Created author: $AUTHOR_ID"

# 3. Create article
ARTICLE_ID=$(curl -s "http://localhost:9000/api/v1/blog/create_article?author_id=$AUTHOR_ID&title=Hello%20World&content=My%20first%20post&is_published=true" | jq -r '.data[0].id')
echo "Created article: $ARTICLE_ID"

# 4. Add comment
curl -s "http://localhost:9000/api/v1/blog/comment_on_article?article_id=$ARTICLE_ID&author_name=Bob&author_email=bob@example.com&content=Nice%20post!" | jq

# 5. Get author details with stats
curl -s "http://localhost:9000/api/v1/blog/author_details?id=$AUTHOR_ID" | jq

# 6. List all articles by author
curl -s "http://localhost:9000/api/v1/blog/list_articles_by_author?author_id=$AUTHOR_ID" | jq

# 7. Get system statistics
curl -s "http://localhost:9000/api/v1/blog/stats_overview" | jq
```

### MCP stdio Workflow Script

Save as `test-mcp-stdio.sh`:

```bash
#!/bin/bash

# Start the server
java -jar target/blog-app-1.0-SNAPSHOT.jar \
  --config blog-mcp-stdio.yaml \
  --log-file /tmp/blog-mcp-stdio.log \
  --redirect-stderr &

SERVER_PID=$!
sleep 2  # Wait for server to start

# Send commands via stdin
{
  echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}'
  sleep 1
  echo '{"jsonrpc":"2.0","method":"notifications/initialized"}'
  sleep 1
  echo '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}'
  sleep 1
  echo '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"create_author","arguments":{"username":"test_user","email":"test@example.com"}}}'
  sleep 1
  echo '{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"list_authors","arguments":{"limit":10,"page":1}}}'
} | nc localhost 9000

# Clean up
kill $SERVER_PID
```

---

## Performance Testing

### Load Testing REST API

Using Apache Bench:

```bash
# Test create_author endpoint
ab -n 1000 -c 10 "http://localhost:9000/api/v1/blog/list_authors?limit=10&page=1"

# Results show requests/second, latency, etc.
```

Using curl with timing:

```bash
# Measure response time
time curl -s "http://localhost:9000/api/v1/blog/stats_overview" > /dev/null
```

---

## Validation Testing

### Test Input Validation

#### Invalid Username (too short)

```bash
curl "http://localhost:9000/api/v1/blog/create_author?username=ab&email=test@example.com"
# Should return validation error
```

#### Invalid Email Format

```bash
curl "http://localhost:9000/api/v1/blog/create_author?username=testuser&email=invalid-email"
# Should return validation error
```

#### Missing Required Fields

```bash
curl "http://localhost:9000/api/v1/blog/create_author?username=testuser"
# Should return error: email required
```

---

## Quick Reference

### Available Tools (15 total)

| Tool | Description |
|------|-------------|
| `create_author` | Create new author |
| `update_author` | Update author information |
| `delete_author` | Delete author (cascades) |
| `list_authors` | List authors with pagination |
| `author_details` | Get author with statistics |
| `create_article` | Create new article |
| `update_article` | Update article |
| `list_articles_by_author` | List articles by author |
| `comment_on_article` | Add comment to article |
| `update_comment` | Update comment content |
| `list_comments_by_article` | List article comments |
| `stats_overview` | Get system statistics |
| `get_api_documentation` | Get API docs |
| `get_openapi_spec` | Get OpenAPI spec |
| `get_postman_collection` | Get Postman collection |

### Useful Commands

```bash
# Check if server is running
curl -s http://localhost:9000/health

# Format JSON output
curl -s <url> | jq

# Save response to file
curl -s <url> -o response.json

# Verbose output (see headers)
curl -v <url>

# Follow redirects
curl -L <url>

# Measure request time
curl -w "@curl-timing.txt" -o /dev/null -s <url>
```

Create `curl-timing.txt`:
```
time_namelookup:  %{time_namelookup}\n
time_connect:     %{time_connect}\n
time_starttransfer: %{time_starttransfer}\n
time_total:       %{time_total}\n
```

---

## Summary

This testing guide covers:
- ✅ **Database Setup**: H2 in-memory, PostgreSQL with Docker, custom test data generation
- ✅ **REST API Testing**: Complete curl examples for all endpoints
- ✅ **MCP HTTP Testing**: Session management and SSE streaming
- ✅ **MCP stdio Testing**: JSON-RPC communication with stdin/stdout
- ✅ **Visual Testing**: MCP Inspector web UI for interactive debugging
- ✅ **AI Integration**: Claude Desktop configuration and testing
- ✅ **Troubleshooting**: 13+ common issues with detailed solutions
- ✅ **Complete Workflows**: End-to-end testing scenarios
- ✅ **Performance Testing**: Load testing and validation examples

### Key Files and Resources

| Resource | Description |
|----------|-------------|
| [README.md](README.md) | Full application documentation with architecture |
| [TESTING.md](TESTING.md) | This comprehensive testing guide |
| [API_DOCUMENTATION.md](API_DOCUMENTATION.md) | API usage guide with code generation |
| [openapi-blog-api.yaml](openapi-blog-api.yaml) | OpenAPI 3.0.3 specification |
| [postman-api-complete.json](postman-api-complete.json) | Postman Collection v2.1 |
| `infra/docker-compose.yml` | PostgreSQL Docker setup |
| `infra/postgres/generate_test_data.py` | Test data generation script |
| `infra/postgres/populate_db.py` | Database population script |

### Quick Reference Commands

```bash
# Start with H2 (instant, no setup)
java -jar target/blog-app-1.0-SNAPSHOT.jar --config blog-rest.yaml

# Start with PostgreSQL
cd infra && docker-compose up -d && cd ..
java -jar target/blog-app-1.0-SNAPSHOT.jar --config blog-rest.yaml

# Generate custom test data
cd infra/postgres
python populate_db.py --generate --authors 100 --articles 500 --comments 2000

# Test with Claude Desktop (WSL2)
wsl bash /path/to/blog-app-claude.sh

# Test with MCP Inspector
npx @modelcontextprotocol/inspector -- java -jar target/blog-app-1.0-SNAPSHOT.jar \
  --config blog-mcp-stdio.yaml --log-file /tmp/blog-mcp-stdio.log --redirect-stderr
```


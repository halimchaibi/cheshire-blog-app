# Blog Application with Cheshire Framework

A fully functional blog application built using the Cheshire framework, demonstrating how to expose database operations through multiple protocols (REST API, MCP stdio, MCP HTTP).

## Features

- **Authors Management**: Create, update, delete, and query blog authors
- **Articles Management**: Full CRUD operations for blog articles with publish status
- **Comments System**: Create and manage comments on articles
- **Multiple Exposure Types**: REST API, MCP stdio, and MCP streamable HTTP
- **H2 In-Memory Database**: Quick startup with pre-populated test data
- **Functional Java**: Follows immutable, declarative programming patterns

## Architecture

The application uses the Cheshire framework's three-stage pipeline architecture:

```
Input → PreProcessor → Executor → PostProcessor → Output
```

### Components

- **BlogApp**: Main application class with configuration selector
- **BlogInputProcessor**: Input validation and transformation
- **BlogExecutor**: SQL query building and execution using JDBC
- **BlogOutputProcessor**: Response formatting

### Configuration

- **blog-application.yaml**: Core application configuration with data sources and capabilities
- **blog-rest.yaml**: REST API exposure configuration
- **blog-mcp-stdio.yaml**: Standard I/O MCP configuration
- **blog-mcp-streamable-http.yaml**: HTTP-based MCP configuration
- **blog-actions.yaml**: MCP tools specification (15 operations)
- **blog-pipelines.yaml**: Pipeline definitions with DSL queries

## Database Schema

The application uses an H2 in-memory database with the following tables:

- **Authors**: Blog authors with username and email
- **Articles**: Blog articles with title, content, and publication status
- **Comments**: Comments on articles with author information

## Operations

### Authors (5 operations)
- `create_author(username, email)` - Create a new author
- `update_author(id, username, email)` - Update author information
- `delete_author(id)` - Delete an author (cascade deletes articles)
- `get_author(id)` - Retrieve a single author
- `list_authors(limit, offset)` - List all authors with pagination

### Articles (7 operations)
- `create_article(title, content, author_id, is_published)` - Create a new article
- `update_article(id, title, content, is_published)` - Update an article
- `delete_article(id)` - Delete an article (cascade deletes comments)
- `get_article(id)` - Retrieve a single article
- `list_articles(limit, offset)` - List all articles with pagination
- `list_published_articles(limit, offset)` - List only published articles
- `search_articles(query, limit)` - Search articles by title or content

### Comments (3 operations)
- `create_comment(article_id, author_name, author_email, content)` - Create a comment
- `delete_comment(id)` - Delete a comment
- `list_comments_by_article(article_id, limit, offset)` - List comments for an article

## Prerequisites

- Java 21 or higher
- Maven 3.6 or higher
- Cheshire framework installed locally (1.0-SNAPSHOT)

## Building

```bash
cd /home/hchaibi/workspace/idea-projects/cheshire-framework/blog-app
mvn clean install
```

## Running

The application accepts a command-line argument to select the exposure type:

### REST API Mode (Default)

```bash
java -jar target/blog-app-1.0-SNAPSHOT.jar --rest
```

Access the REST API at: `http://localhost:8080/api/v1`

Example requests:
```bash
# List all authors
curl http://localhost:8080/api/v1/authors

# Get a specific author
curl http://localhost:8080/api/v1/authors/{id}

# Create an author
curl -X POST http://localhost:8080/api/v1/authors \
  -H "Content-Type: application/json" \
  -d '{"username":"new_author","email":"new@example.com"}'

# List published articles
curl http://localhost:8080/api/v1/articles/published
```

### MCP stdio Mode

```bash
java -jar target/blog-app-1.0-SNAPSHOT.jar --mcp-stdio
```

This mode exposes the application through standard I/O for MCP clients.

### MCP HTTP Mode

```bash
java -jar target/blog-app-1.0-SNAPSHOT.jar --mcp-http
```

Access the MCP service at: `http://localhost:8080/mcp/v1`

## Project Structure

```
blog-app/
├── pom.xml
├── README.md
├── db.sql (original MySQL schema)
├── src/
│   └── main/
│       ├── java/
│       │   └── io/
│       │       └── blog/
│       │           ├── BlogApp.java
│       │           └── pipeline/
│       │               ├── BlogInputProcessor.java
│       │               ├── BlogExecutor.java
│       │               └── BlogOutputProcessor.java
│       └── resources/
│           ├── schema.sql (H2-compatible schema)
│           ├── simplelogger.properties
│           └── config/
│               ├── blog-application.yaml
│               ├── blog-rest.yaml
│               ├── blog-mcp-stdio.yaml
│               ├── blog-mcp-streamable-http.yaml
│               ├── blog-actions.yaml
│               └── blog-pipelines.yaml
```

## Development

### Code Style

The application follows functional programming principles:
- Immutability by default (all fields `final`)
- No nulls (use `Optional<T>`)
- Declarative streams over imperative loops
- Small, focused methods with single responsibility
- Records for DTOs

### Adding New Operations

1. Add the tool definition to `blog-actions.yaml`
2. Create the pipeline definition in `blog-pipelines.yaml`
3. The existing processors will handle the new operation automatically

### Extending the Database

1. Update `schema.sql` with new tables
2. Add corresponding operations to the configuration files
3. No Java code changes required (declarative configuration)

## Testing

The database is pre-populated with test data:
- 8 test authors
- 9 articles (7 published, 2 drafts)
- 10 comments on various articles

## Logging

Logging is configured in `simplelogger.properties`:
- Application logs: DEBUG level
- Cheshire framework: INFO level
- Jetty server: WARN level

## Troubleshooting

### Port Already in Use

Change the port in `blog-application.yaml`:
```yaml
transports:
  jetty:
    config:
      port: 8081  # Change to desired port
```

### Database Connection Issues

The H2 database is in-memory and initialized on startup. No external database setup is required.

### Configuration Not Found

Ensure you're running from the correct directory or the JAR file includes the resources.

## Reference Implementation

This application is based on the Chinook reference implementation located at:
`/home/hchaibi/workspace/idea-projects/cheshire-framework/cheshire-reference-impl`

## License

This is a demonstration application for the Cheshire framework.


# Blog Application with Cheshire Framework

A sample blog application built using the Cheshire framework, demonstrating how to expose database operations through multiple protocols (REST API, MCP stdio, MCP HTTP).

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

## Running the Application

The application supports three different exposure modes, each optimized for different use cases. Select the appropriate mode based on your integration requirements.

### 🌐 Mode 1: REST API (Default)

**Use Case**: Traditional REST API for web applications, mobile apps, and HTTP clients.

**Configuration**: `blog-rest.yaml`

**Start Command**:
```bash
java -jar target/blog-app-1.0-SNAPSHOT.jar --config blog-rest.yaml
```

Or simply (blog-rest.yaml is the default):
```bash
java -jar target/blog-app-1.0-SNAPSHOT.jar
```

Or from Maven:
```bash
mvn exec:java -Dexec.mainClass="io.blog.BlogApp" -Dexec.args="--config blog-rest.yaml"
```

**Service URL**: `http://localhost:8080/api/v1/blog`

**Features**:
- Full RESTful HTTP interface
- JSON request/response format
- HTTP methods: GET, POST, PUT, DELETE
- Standard HTTP status codes
- CORS enabled for web clients

**Testing REST API**:

```bash
# List all authors with pagination
curl "http://localhost:8080/api/v1/blog/list_authors?page=1&limit=10"

# Get specific author details with statistics
curl "http://localhost:8080/api/v1/blog/author_details?id=<author-uuid>"

# Create a new author
curl "http://localhost:8080/api/v1/blog/create_author?username=john_doe&email=john@example.com"

# Update an author
curl "http://localhost:8080/api/v1/blog/update_author?id=<author-uuid>&email=newemail@example.com"

# Create an article
curl "http://localhost:8080/api/v1/blog/create_article?author_id=<author-uuid>&title=My%20Article&content=Article%20content%20here&is_published=true"

# List articles by author with filtering
curl "http://localhost:8080/api/v1/blog/list_articles_by_author?author_id=<author-uuid>&published=true&limit=20"

# Add a comment to an article
curl "http://localhost:8080/api/v1/blog/comment_on_article?article_id=<article-uuid>&author_name=Jane&content=Great%20article!"

# Get comprehensive system statistics
curl "http://localhost:8080/api/v1/blog/stats_overview"

# Get statistics for a specific timeframe
curl "http://localhost:8080/api/v1/blog/stats_overview?timeframe=month"
```

**Using Postman**:
1. Import the Postman collection: `postman-api-complete.json`
2. Set environment variable `url` to `http://localhost:9000`
3. Run requests from the organized folder structure

**Using OpenAPI/Swagger**:
1. Access OpenAPI spec: `openapi-blog-api.yaml`
2. Import into Swagger UI or Redoc
3. Interactive API testing and documentation

**Configuration Options** (`blog-rest.yaml`):
```yaml
transports:
  jetty:
    config:
      port: 9000          # HTTP port (change if needed)
      host: "0.0.0.0"     # Bind address
      maxThreads: 200     # Thread pool size
```

---

### 📟 Mode 2: MCP stdio (Standard Input/Output)

**Use Case**: Integration with AI assistants (Claude Desktop), command-line tools, and process-based integrations.

**Configuration**: `blog-mcp-stdio.yaml`

**Start Command**:
```bash
java -jar target/blog-app-1.0-SNAPSHOT.jar --config blog-mcp-stdio.yaml
```

Or from Maven:
```bash
mvn exec:java -Dexec.mainClass="io.blog.BlogApp" -Dexec.args="--config blog-mcp-stdio.yaml"
```

**Protocol**: Model Context Protocol (MCP) over stdin/stdout

**Features**:
- JSON-RPC 2.0 protocol
- Bidirectional communication over stdio
- Tool discovery and invocation
- Resource templates
- Prompt templates for AI assistants
- No network ports required

**MCP Capabilities**:
- **15 Tools**: All CRUD operations, queries, and statistics
- **4 Resource Templates**: Dynamic data access
- **10 Static Resources**: Schemas, documentation, OpenAPI spec
- **5 Prompts**: AI-assisted workflows

**Integration with Claude Desktop**:

Add to your Claude Desktop MCP configuration (`claude_desktop_config.json`):

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

**⚠️ CRITICAL: Stderr Redirection for MCP stdio**

When running in MCP stdio mode, you **MUST** use the `--redirect-stderr` (or `-r`) flag. This is essential because:

1. **MCP Protocol Requirement**: MCP stdio uses standard input/output (stdin/stdout) for JSON-RPC 2.0 communication
2. **SimpleLogger Configuration**: The application uses SLF4J SimpleLogger which outputs to `System.err` by default (configured in `simplelogger.properties`)
3. **Communication Pollution**: Without redirection, log messages on stderr will interfere with the JSON-RPC messages on stdout, causing parsing errors and protocol failures

**Why This Matters**:
```properties
# From simplelogger.properties
org.slf4j.simpleLogger.logFile=System.err  # ← Logs go to stderr!
org.slf4j.simpleLogger.defaultLogLevel=debug
```

If stderr is not redirected, debug logs will corrupt the MCP communication channel:
```bash
# WITHOUT --redirect-stderr (BROKEN - logs pollute stdio)
{"jsonrpc":"2.0","method":"tools/list"}
2026-01-12 10:30:45.123 [main] DEBUG io.cheshire - Initializing...  # ← Breaks JSON parsing!
{"result":{"tools":[...]}}

# WITH --redirect-stderr (CORRECT - logs go to file)
{"jsonrpc":"2.0","method":"tools/list"}
{"result":{"tools":[...]}}
# Logs are safely written to /tmp/blog-mcp-stdio.log
```

**Correct Usage**:
```bash
java -jar target/blog-app-1.0-SNAPSHOT.jar \
  --config blog-mcp-stdio.yaml \
  --log-file /tmp/blog-mcp-stdio.log \
  --redirect-stderr  # ← REQUIRED for MCP stdio!
```

---

**🪟 WSL2 with Claude Desktop on Windows**

If you're running WSL2 (Ubuntu) and Claude Desktop on Windows, use the provided script for easy configuration:

**Script Location**: `dev-utils/blog-app-claude.sh`

```bash
#!/bin/bash
/usr/lib/jvm/java-21-openjdk-amd64/bin/java --enable-preview \
  -jar /home/hchaibi/workspace/idea-projects/cheshire-framework/cheshire-blog-app/target/blog-app-1.0-SNAPSHOT.jar \
  --config blog-mcp-stdio.yaml \
  --log-file /tmp/blog-mcp-stdio.log \
  --redirect-stderr
```

**Claude Desktop Configuration** (`claude_desktop_config.json` on Windows):

```json
{
  "mcpServers": {
    "blog-api": {
      "command": "wsl",
      "args": [
        "bash",
        "/home/hchaibi/workspace/idea-projects/cheshire-framework/cheshire-blog-app/dev-utils/blog-app-claude.sh"
      ]
    }
  }
}
```

**Or using direct WSL path** (Windows-style path):

```json
{
  "mcpServers": {
    "blog-api": {
      "command": "wsl",
      "args": [
        "bash",
        "\\\\wsl.localhost\\Ubuntu-22.04\\home\\hchaibi\\workspace\\idea-projects\\cheshire-framework\\cheshire-blog-app\\dev-utils\\blog-app-claude.sh"
      ]
    }
  }
}
```

**Make script executable**:
```bash
chmod +x /home/hchaibi/workspace/idea-projects/cheshire-framework/cheshire-blog-app/dev-utils/blog-app-claude.sh
```

**Verify logs**:
```bash
# Monitor logs in real-time
tail -f /tmp/blog-mcp-stdio.log

# Check for errors
grep ERROR /tmp/blog-mcp-stdio.log
```

**Troubleshooting WSL2 Setup**:

1. **Java not found**: Ensure Java 21 is installed in WSL2
   ```bash
   wsl java -version
   ```

2. **Script not executable**: 
   ```bash
   wsl chmod +x /path/to/blog-app-claude.sh
   ```

3. **Path issues**: Use absolute paths in the script

4. **Claude can't connect**: Check logs at `/tmp/blog-mcp-stdio.log`

5. **Test manually**:
   ```bash
   wsl bash /path/to/blog-app-claude.sh
   # Then send: {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}
   ```

**Testing MCP stdio**:

Using manual JSON-RPC (for development):

```bash
# Start the server (CRITICAL: use --redirect-stderr to avoid log pollution!)
java -jar target/blog-app-1.0-SNAPSHOT.jar \
  --config blog-mcp-stdio.yaml \
  --log-file /tmp/blog-mcp-stdio.log \
  --redirect-stderr

# Send initialize request (via stdin)
{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test-client","version":"1.0"}}}

# List available tools
{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}

# Call a tool (list authors)
{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"list_authors","arguments":{"page":1,"limit":10}}}

# Access a resource
{"jsonrpc":"2.0","id":4,"method":"resources/read","params":{"uri":"docs://api_documentation"}}

# List prompts
{"jsonrpc":"2.0","id":5,"method":"prompts/list","params":{}}
```

**🛠️ MCP Inspector Tool** (Visual debugging):

Use the MCP Inspector for interactive testing and debugging:

```bash
# Install MCP Inspector (if not already installed)
npm install -g @modelcontextprotocol/inspector

# Run with inspector (script provided in dev-utils/)
cd idea-projects/cheshire-framework/cheshire-blog-app
bash dev-utils/blog-inspector.sh

# Or manually:
npx @modelcontextprotocol/inspector -- java -jar target/blog-app-1.0-SNAPSHOT.jar \
  --config blog-mcp-stdio.yaml \
  --log-file /tmp/blog-mcp-stdio.log \
  --redirect-stderr
```

The inspector provides a web UI at `http://localhost:5173` for:
- Interactive tool testing
- Request/response inspection
- Protocol debugging
- Resource exploration

**Available MCP Tools** (see `blog-actions-v2.yaml`):
- `create_author`, `update_author`, `delete_author`
- `list_authors`, `author_details`
- `create_article`, `update_article`, `list_articles_by_author`
- `comment_on_article`, `update_comment`, `list_comments_by_article`
- `stats_overview`
- `get_api_documentation`, `get_openapi_spec`, `get_postman_collection`

**Claude Usage Examples**:

Once configured, ask Claude:
- "Show me all authors with at least 5 articles"
- "Create a new author named John Smith with email john@example.com"
- "List all articles by author John with statistics"
- "Get comprehensive blog statistics for the last month"
- "Show me the API documentation"

---

### 🌊 Mode 3: MCP HTTP Streamable (Server-Sent Events)

**Use Case**: Web-based MCP clients, browser integrations, and HTTP-based AI assistants with streaming support.

**Configuration**: `blog-mcp-streamable-http.yaml`

**Start Command**:
```bash
java -jar target/blog-app-1.0-SNAPSHOT.jar --config blog-mcp-streamable-http.yaml
```

Or from Maven:
```bash
mvn exec:java -Dexec.mainClass="io.blog.BlogApp" -Dexec.args="--config blog-mcp-streamable-http.yaml"
```

**Service URLs**:
- **MCP Endpoint**: `http://localhost:8080/mcp`
- **SSE Stream**: `http://localhost:8080/mcp/sse`
- **Health Check**: `http://localhost:8080/health`

**Protocol**: MCP over HTTP with Server-Sent Events (SSE)

**Features**:
- HTTP-based MCP protocol
- Server-Sent Events for streaming responses
- Long-running operation support
- Progress updates via SSE
- WebSocket-like experience over HTTP
- Cross-origin support (CORS)

**Testing MCP HTTP**:

```bash
# Initialize MCP session
curl -X POST http://localhost:8080/mcp/initialize \
  -H "Content-Type: application/json" \
  -d '{
    "protocolVersion": "2024-11-05",
    "capabilities": {},
    "clientInfo": {"name": "test-client", "version": "1.0"}
  }'

# List available tools
curl http://localhost:8080/mcp/tools/list

# Call a tool (create author)
curl -X POST http://localhost:8080/mcp/tools/call \
  -H "Content-Type: application/json" \
  -d '{
    "name": "create_author",
    "arguments": {
      "username": "jane_smith",
      "email": "jane@example.com"
    }
  }'

# List resources
curl http://localhost:8080/mcp/resources/list

# Read a resource (OpenAPI spec)
curl -X POST http://localhost:8080/mcp/resources/read \
  -H "Content-Type: application/json" \
  -d '{"uri": "docs://openapi_spec"}'

# Access SSE stream (for streaming responses)
curl -N http://localhost:8080/mcp/sse
```

**Using with JavaScript/Browser**:

```javascript
// Initialize MCP client
const mcpClient = new MCPClient('http://localhost:8080/mcp');

// Initialize session
await mcpClient.initialize({
  protocolVersion: '2024-11-05',
  capabilities: {},
  clientInfo: { name: 'web-client', version: '1.0' }
});

// List tools
const tools = await mcpClient.listTools();
console.log('Available tools:', tools);

// Call a tool
const result = await mcpClient.callTool('list_authors', {
  page: 1,
  limit: 10
});
console.log('Authors:', result);

// Subscribe to SSE stream for real-time updates
const eventSource = new EventSource('http://localhost:8080/mcp/sse');
eventSource.onmessage = (event) => {
  console.log('SSE Event:', JSON.parse(event.data));
};
```

**Configuration Options** (`blog-mcp-streamable-http.yaml`):
```yaml
transports:
  jetty:
    config:
      port: 8080
      host: "0.0.0.0"
      enableSSE: true        # Server-Sent Events
      sseHeartbeat: 30000    # Heartbeat interval (ms)
      cors:
        enabled: true
        allowedOrigins: "*"
```

---

## Mode Comparison

| Feature | REST API | MCP stdio | MCP HTTP |
|---------|----------|-----------|----------|
| **Protocol** | HTTP/REST | JSON-RPC over stdio | MCP over HTTP/SSE |
| **Network** | ✅ Yes (HTTP) | ❌ No (local process) | ✅ Yes (HTTP) |
| **Use Case** | Web/Mobile Apps | CLI tools, AI assistants | Web-based AI, Browsers |
| **Streaming** | ❌ No | ✅ Yes (bidirectional) | ✅ Yes (SSE) |
| **Tool Discovery** | ❌ No (static API) | ✅ Yes (MCP) | ✅ Yes (MCP) |
| **Documentation** | OpenAPI/Swagger | MCP Resources | MCP Resources |
| **Best For** | Traditional apps | Claude Desktop | Web integrations |
| **Port Required** | 9000 | None | 9000 |
| **Stderr Redirect** | Optional | ⚠️ **REQUIRED** | Optional |
| **Log File** | Optional | ⚠️ **REQUIRED** | Optional |

---

## Advanced Configuration

### Command-Line Options

The application supports the following CLI options:

```bash
# Show help
java -jar target/blog-app-1.0-SNAPSHOT.jar --help

# Specify configuration file (default: blog-rest.yaml)
java -jar target/blog-app-1.0-SNAPSHOT.jar --config blog-mcp-stdio.yaml

# Specify log file path (default: /tmp/blog-app.log)
java -jar target/blog-app-1.0-SNAPSHOT.jar --log-file /var/log/blog-app.log

# Enable runtime metrics logging
java -jar target/blog-app-1.0-SNAPSHOT.jar --log-metrics

# Redirect stderr to log file
java -jar target/blog-app-1.0-SNAPSHOT.jar --redirect-stderr --log-file /var/log/blog-app.log

# Combine multiple options
java -jar target/blog-app-1.0-SNAPSHOT.jar \
  --config blog-rest.yaml \
  --log-file /var/log/blog-app.log \
  --log-metrics \
  --redirect-stderr
```

**Available Options**:
- `-c, --config <file>` - Path to configuration file (default: `blog-rest.yaml`)
- `-l, --log-file <path>` - Log file path (default: `/tmp/blog-app.log`)
- `-m, --log-metrics` - Enable runtime metrics logging (logs every 20 seconds)
- `-r, --redirect-stderr` - Redirect stderr to log file
- `-h, --help` - Show help message
- `-V, --version` - Show version information

### Environment Variables

```bash
# Set log level
export LOG_LEVEL=DEBUG

# Set custom port
export SERVER_PORT=9000

# Set database location (if using file-based H2)
export DB_PATH=/path/to/database

# Run with environment
java -jar target/blog-app-1.0-SNAPSHOT.jar --config blog-rest.yaml
```

### JVM Options

```bash
# Increase heap size for large datasets
java -Xmx2g -Xms512m -jar target/blog-app-1.0-SNAPSHOT.jar --config blog-rest.yaml

# Enable remote debugging
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 \
  -jar target/blog-app-1.0-SNAPSHOT.jar --config blog-rest.yaml
```

### Running in Background

```bash
# Run as background process (Linux/Mac)
nohup java -jar target/blog-app-1.0-SNAPSHOT.jar --config blog-rest.yaml > app.log 2>&1 &

# Get process ID
echo $! > app.pid

# Stop the application
kill $(cat app.pid)
```

---

---

## 🧪 Testing

For comprehensive testing instructions, see **[TESTING.md](TESTING.md)**.

The testing guide includes:
- ✅ **Database Setup**: H2 in-memory, PostgreSQL with Docker, custom test data generation
- ✅ **REST API Testing**: Complete curl examples for all endpoints
- ✅ **MCP HTTP Testing**: Session management and tool invocation
- ✅ **MCP stdio Testing**: JSON-RPC communication examples
- ✅ **MCP Inspector**: Visual debugging with web UI
- ✅ **Claude Desktop**: AI integration testing
- ✅ **Troubleshooting**: 13+ solutions for common issues
- ✅ **Validation Testing**: Input validation examples
- ✅ **Performance Testing**: Load testing with custom datasets

### Quick Start Testing

```bash
# Option 1: Use H2 in-memory database (instant, pre-populated)
java -jar target/blog-app-1.0-SNAPSHOT.jar --config blog-rest.yaml

# Option 2: Use PostgreSQL with Docker (persistent data)
cd infra
docker-compose up -d
cd ..
java -jar target/blog-app-1.0-SNAPSHOT.jar --config blog-rest.yaml

# Option 3: Generate custom test data for performance testing
cd infra/postgres
source venv/bin/activate
python populate_db.py --generate --authors 500 --articles 5000 --comments 25000
```

---

## Quick Start Examples

### Example 1: REST API Workflow
```bash
# Start REST API
java -jar target/blog-app-1.0-SNAPSHOT.jar --config blog-rest.yaml

# Create author
AUTHOR_ID=$(curl -s "http://localhost:8080/api/v1/blog/create_author?username=demo_user&email=demo@example.com" | jq -r '.data[0].id')

# Create article
ARTICLE_ID=$(curl -s "http://localhost:8080/api/v1/blog/create_article?author_id=$AUTHOR_ID&title=My%20First%20Post&content=Hello%20World&is_published=true" | jq -r '.data[0].id')

# Add comment
curl "http://localhost:8080/api/v1/blog/comment_on_article?article_id=$ARTICLE_ID&author_name=Reader&content=Great%20post!"

# View statistics
curl "http://localhost:8080/api/v1/blog/stats_overview" | jq
```

### Example 2: MCP stdio with Claude
```bash
# Start MCP stdio server (REMEMBER: --redirect-stderr is REQUIRED!)
java -jar target/blog-app-1.0-SNAPSHOT.jar \
  --config blog-mcp-stdio.yaml \
  --log-file /tmp/blog-mcp-stdio.log \
  --redirect-stderr

# Or use the WSL2 script for Windows + Claude Desktop
wsl bash /home/hchaibi/workspace/idea-projects/cheshire-framework/cheshire-blog-app/dev-utils/blog-app-claude.sh

# Add to Claude Desktop config (see WSL2 section above)
# Then ask Claude: "Using the blog API, create a new author named Alice with email alice@example.com"
# Claude will automatically discover and use the MCP tools
```

### Example 3: MCP HTTP Testing
```bash
# Start MCP HTTP server
java -jar target/blog-app-1.0-SNAPSHOT.jar --config blog-mcp-streamable-http.yaml

# Use curl to interact
curl -X POST http://localhost:8080/mcp/tools/call \
  -H "Content-Type: application/json" \
  -d '{"name":"list_authors","arguments":{"limit":5}}' | jq
```

## Project Structure

```
blog-app/
├── pom.xml
├── README.md
├── TESTING.md (comprehensive testing guide)
├── API_DOCUMENTATION.md (API usage guide)
├── openapi-blog-api.yaml (OpenAPI 3.0 specification)
├── postman-api-complete.json (Postman collection)
├── db.sql (original MySQL schema)
├── dev-utils/
│   ├── blog-app-claude.sh (WSL2 Claude Desktop launcher)
│   └── blog-inspector.sh (MCP Inspector launcher)
├── infra/
│   ├── docker-compose.yml (PostgreSQL Docker setup)
│   ├── schema.sql (H2-compatible schema with test data)
│   └── postgres/ (test data generation scripts)
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

### MCP stdio: JSON Parse Errors / Communication Failures

**Symptom**: Claude Desktop shows connection errors, or manual JSON-RPC requests fail with parse errors.

**Cause**: Log messages from SimpleLogger are polluting the stdio channel.

**Solution**: **ALWAYS** use `--redirect-stderr` flag for MCP stdio mode:
```bash
java -jar target/blog-app-1.0-SNAPSHOT.jar \
  --config blog-mcp-stdio.yaml \
  --log-file /tmp/blog-mcp-stdio.log \
  --redirect-stderr  # ← CRITICAL!
```

**Why**: SimpleLogger outputs to `System.err` by default (`simplelogger.properties`):
```properties
org.slf4j.simpleLogger.logFile=System.err  # Conflicts with MCP stdio!
```

**Verify**: Check if stderr redirection is working:
```bash
# Run without redirection (WRONG - will see logs mixed with JSON)
java -jar target/blog-app-1.0-SNAPSHOT.jar --config blog-mcp-stdio.yaml
# Output: 2026-01-12 10:30:45.123 [main] DEBUG io.cheshire - Starting...  ← BAD!

# Run with redirection (CORRECT - only JSON output visible)
java -jar target/blog-app-1.0-SNAPSHOT.jar \
  --config blog-mcp-stdio.yaml \
  --log-file /tmp/blog-mcp-stdio.log \
  --redirect-stderr
# Output: {"jsonrpc":"2.0",...}  ← GOOD! (logs go to /tmp/blog-mcp-stdio.log)
```

### Port Already in Use

Change the port in `blog-application.yaml`:
```yaml
transports:
  jetty:
    config:
      port: 9001  # Change to desired port
```

### Database Connection Issues

The H2 database is in-memory and initialized on startup. No external database setup is required.

### Configuration Not Found

Ensure you're running from the correct directory or the JAR file includes the resources.

### WSL2: Java Command Not Found

Install Java 21 in WSL2:
```bash
sudo apt update
sudo apt install openjdk-21-jdk
java -version  # Verify installation
```

### WSL2: Script Permission Denied

Make the script executable:
```bash
chmod +x /home/hchaibi/workspace/idea-projects/cheshire-framework/cheshire-blog-app/dev-utils/blog-app-claude.sh
```

## Documentation

The Blog Application includes comprehensive documentation:

| Document | Description |
|----------|-------------|
| [README.md](README.md) | Main documentation with architecture, configuration, and quick start |
| [TESTING.md](TESTING.md) | Complete testing guide for all modes (REST, MCP stdio, MCP HTTP) |
| [API_DOCUMENTATION.md](API_DOCUMENTATION.md) | API usage guide with code generation examples |
| [openapi-blog-api.yaml](openapi-blog-api.yaml) | OpenAPI 3.0.3 specification for Swagger/Redoc |
| [postman-api-complete.json](postman-api-complete.json) | Postman Collection v2.1 with all endpoints |

### Framework Documentation

| Document | Location | Description |
|----------|----------|-------------|
| Pipeline Configuration Guide | [cheshire-prototype/docs/guides/user/PIPELINE_CONFIGURATION_GUIDE.md](../cheshire-prototype/docs/guides/user/PIPELINE_CONFIGURATION_GUIDE.md) | Comprehensive guide for configuring pipelines with DSL reference |

### Configuration Files

| File | Purpose |
|------|---------|
| `blog-application.yaml` | Core application config (datasource, capabilities) |
| `blog-pipelines.yaml` | Pipeline definitions with validation rules |
| `blog-actions.yaml` | MCP tools specification (15 operations) |
| `blog-rest.yaml` | REST API exposure configuration |
| `blog-mcp-stdio.yaml` | MCP stdio configuration for CLI/AI |
| `blog-mcp-streamable-http.yaml` | MCP HTTP with SSE configuration |

## Reference Implementation

This application is based on the Chinook reference implementation located at:
`/home/hchaibi/workspace/idea-projects/cheshire-framework/cheshire-reference-impl`

## License

This is a demonstration application for the Cheshire framework.


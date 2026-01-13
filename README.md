# Blog Application with Cheshire Framework

A comprehensive reference implementation built with the Cheshire framework, demonstrating how to expose database operations through multiple protocols. The application provides **15 operations** across Authors, Articles, Comments, Statistics, and Documentation endpoints, accessible via **REST API**, **MCP stdio** (Claude Desktop), and **MCP HTTP** with streaming.

This application serves as the primary example for learning and implementing the Cheshire framework's capabilities, including three-stage pipelines, DSL query templates, multi-protocol support, and LLM integration.

## Features

- **15 Operations**: Complete API with Authors (5), Articles (3), Comments (3), Statistics (1), and Documentation (3) endpoints
- **Multi-Protocol Support**: REST API, MCP stdio (for Claude Desktop), and MCP HTTP with streaming
- **Dual Database Support**: PostgreSQL with Docker Compose OR H2 in-memory (pre-populated with test data)
- **Comprehensive Testing**: 1,800+ lines of testing documentation with curl examples and workflow tests
- **OpenAPI 3.0**: Complete API specification with schemas, examples, and validation rules
- **Postman Collection**: Ready-to-use collection with 15+ requests and environment variables
- **LLM-Ready**: MCP integration for AI agents (Claude Desktop) with tool discovery
- **Pipeline Architecture**: Three-stage processing (PreProcessor → Executor → PostProcessor)
- **DSL Query Templates**: Type-safe SQL generation with parameter binding and filtering
- **Functional Java**: Immutable, declarative programming following Scala-influenced patterns

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

### Configuration Files

Located in `src/main/resources/config/`:

- **blog-application.yaml**: Core application configuration with data sources and capabilities
- **blog-rest.yaml**: REST API exposure on port 9000 (`/api/v1/blog`)
- **blog-mcp-stdio.yaml**: Standard I/O MCP for Claude Desktop integration
- **blog-mcp-streamable-http.yaml**: HTTP-based MCP with streaming on port 9000 (`/mcp/v1`)
- **blog-actions.yaml**: MCP tools specification (15 operations with schemas and validation)
- **blog-pipelines.yaml**: Pipeline definitions with DSL query templates for all operations

## Database Schema

The application uses an H2 in-memory database with the following tables:

- **Authors**: Blog authors with username and email
- **Articles**: Blog articles with title, content, and publication status
- **Comments**: Comments on articles with author information

## Operations

The Blog Application provides **15 operations** organized into five categories:

### Authors (5 operations)
- `create_author(username, email)` - Create a new author with unique username and email
- `update_author(id, username, email)` - Update author information (at least one field required)
- `delete_author(id, confirm)` - Delete an author (cascade deletes all articles and comments)
- `list_authors(page, limit, search_author, created_after, created_before, has_published)` - List authors with filtering and pagination
- `author_details(id)` - Get author with comprehensive statistics (articles, comments, publications)

### Articles (3 operations)
- `create_article(author_id, title, content, is_published)` - Create a new article
- `update_article(id, title, content, is_published)` - Update article (at least one field required)
- `list_articles_by_author(author_id, published, limit, page, sort_by, order)` - List articles by author with filtering and sorting

### Comments (3 operations)
- `comment_on_article(article_id, author_name, author_email, content)` - Add a comment to an article
- `update_comment(id, content)` - Update comment content
- `list_comments_by_article(article_id, limit, page, sort_by, order)` - List comments for an article with pagination

### Statistics (1 operation)
- `stats_overview(timeframe)` - Get comprehensive blog statistics (authors, articles, comments, activity)

### Documentation (3 operations)
- `get_api_documentation()` - Retrieve API documentation in markdown format
- `get_openapi_spec()` - Get OpenAPI 3.0.3 specification
- `get_postman_collection()` - Download Postman Collection v2.1

See [blog-actions.yaml](src/main/resources/config/blog-actions.yaml) for complete operation specifications.

## Prerequisites

- **Java 21 or higher** (with preview features enabled)
- **Maven 3.8 or higher** (or use included Maven wrapper)
- **Git** for cloning repositories

## Setup & Installation

### Step 1: Clone and Install Cheshire Framework

The Blog Application depends on the Cheshire Framework. Install it locally first:

```bash
# Clone the Cheshire Framework
git clone https://github.com/halimchaibi/cheshire-prototype.git
cd cheshire-prototype

# Build and install framework dependencies in local Maven repository
./mvnw clean install

# Or use system Maven if installed
mvn clean install -DskipTests
```

This installs all Cheshire framework modules (cheshire-core, cheshire-runtime, cheshire-server, etc.) to your local Maven repository.

### Step 2: Clone the Blog Application

```bash
# Navigate back to parent directory
cd ..

# Clone the Blog Application
git clone https://github.com/halimchaibi/cheshire-blog-app.git
cd cheshire-blog-app
```

### Step 3: Build the Blog Application

```bash
# Build using Maven wrapper (recommended)
./mvnw clean package

# Or use system Maven
mvn clean package

# The JAR will be created at: target/blog-app-1.0-SNAPSHOT.jar
```

### Quick Start (All-in-One)

```bash
# Clone and setup everything
git clone https://github.com/halimchaibi/cheshire-prototype.git
cd cheshire-prototype
./mvnw clean install -DskipTests
cd ..

git clone https://github.com/halimchaibi/cheshire-blog-app.git
cd cheshire-blog-app
./mvnw clean package

# Run the application
java --enable-preview -jar target/blog-app-1.0-SNAPSHOT.jar --config blog-rest.yaml
```

## Running the Application

The application supports three different exposure modes, each optimized for different use cases. Select the appropriate mode based on your integration requirements.

### 🌐 Mode 1: REST API (Default)

**Use Case**: Traditional REST API for web applications, mobile apps, and HTTP clients.

**Configuration**: `blog-rest.yaml`

⚠️ **Note:** Requires enabling Java preview features.

**Start Command**:
```bash
java --enable-preview -jar target/blog-app-1.0-SNAPSHOT.jar --config blog-rest.yaml
```

Or simply (blog-rest.yaml is the default):
```bash
java --enable-preview -jar target/blog-app-1.0-SNAPSHOT.jar
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
java --enable-preview -jar target/blog-app-1.0-SNAPSHOT.jar --config blog-mcp-stdio.yaml
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
java --enable-preview -jar target/blog-app-1.0-SNAPSHOT.jar \
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
  -jar /path/to/cheshire-blog-app/target/blog-app-1.0-SNAPSHOT.jar \
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
        "/path/to/cheshire-blog-app/dev-utils/blog-app-claude.sh"
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
        "\\\\wsl.localhost\\Ubuntu-22.04\\path\\to\\cheshire-blog-app\\dev-utils\\blog-app-claude.sh"
      ]
    }
  }
}
```

**Make script executable**:
```bash
chmod +x /path/to/cheshire-blog-app/dev-utils/blog-app-claude.sh
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
java --enable-preview -jar target/blog-app-1.0-SNAPSHOT.jar \
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
npx @modelcontextprotocol/inspector -- java --enable-preview -jar target/blog-app-1.0-SNAPSHOT.jar \
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
java --enable-preview -jar target/blog-app-1.0-SNAPSHOT.jar --config blog-mcp-streamable-http.yaml
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
java --enable-preview -jar target/blog-app-1.0-SNAPSHOT.jar --help

# Specify configuration file (default: blog-rest.yaml)
java --enable-preview -jar target/blog-app-1.0-SNAPSHOT.jar --config blog-mcp-stdio.yaml

# Specify log file path (default: /tmp/blog-app.log)
java --enable-preview -jar target/blog-app-1.0-SNAPSHOT.jar --log-file /var/log/blog-app.log

# Enable runtime metrics logging
java --enable-preview -jar target/blog-app-1.0-SNAPSHOT.jar --log-metrics

# Redirect stderr to log file
java --enable-preview -jar target/blog-app-1.0-SNAPSHOT.jar --redirect-stderr --log-file /var/log/blog-app.log

# Combine multiple options
java --enable-preview -jar target/blog-app-1.0-SNAPSHOT.jar \
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
java --enable-preview -jar target/blog-app-1.0-SNAPSHOT.jar --config blog-rest.yaml
```

### JVM Options

```bash
# Increase heap size for large datasets
java -Xmx2g -Xms512m --enable-preview -jar target/blog-app-1.0-SNAPSHOT.jar --config blog-rest.yaml

# Enable remote debugging
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 \
  --enable-preview -jar target/blog-app-1.0-SNAPSHOT.jar --config blog-rest.yaml
```

### Running in Background

```bash
# Run as background process (Linux/Mac)
nohup java --enable-preview -jar target/blog-app-1.0-SNAPSHOT.jar --config blog-rest.yaml > app.log 2>&1 &

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

---

## Project Structure

```
cheshire-blog-app/
├── pom.xml
├── README.md (this file)
├── TESTING.md (comprehensive testing guide - 1,800+ lines)
├── API_DOCUMENTATION.md (API usage guide)
├── openapi-blog-api.yaml (OpenAPI 3.0 specification)
├── postman-api-complete.json (Postman collection)
├── dev-utils/
│   ├── blog-app-claude.sh (WSL2 Claude Desktop launcher)
│   └── blog-inspector.sh (MCP Inspector launcher)
├── infra/
│   ├── docker-compose.yml (PostgreSQL Docker setup)
│   ├── schema.sql (Database schema)
│   └── postgres/ (test data generation scripts with Python)
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
java --enable-preview -jar target/blog-app-1.0-SNAPSHOT.jar \
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
java --enable-preview -jar target/blog-app-1.0-SNAPSHOT.jar --config blog-mcp-stdio.yaml
# Output: 2026-01-12 10:30:45.123 [main] DEBUG io.cheshire - Starting...  ← BAD!

# Run with redirection (CORRECT - only JSON output visible)
java --enable-preview -jar target/blog-app-1.0-SNAPSHOT.jar \
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
chmod +x /path/to/cheshire-blog-app/dev-utils/blog-app-claude.sh
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

This Blog Application serves as the **primary reference implementation** for the Cheshire Framework, demonstrating:

- ✅ **Multi-Protocol Support** - REST API, MCP stdio, and MCP HTTP implementations
- ✅ **Complete CRUD Operations** - 15 operations across 5 categories
- ✅ **Pipeline Configuration** - Comprehensive examples of three-stage pipelines (PreProcessor → Executor → PostProcessor)
- ✅ **DSL Query Templates** - Advanced SQL generation with parameter binding and filtering
- ✅ **Database Integration** - PostgreSQL and H2 in-memory support
- ✅ **Testing & Documentation** - 1,800+ lines of testing guide, OpenAPI spec, and Postman collections
- ✅ **LLM Integration** - Claude Desktop and MCP Inspector integration
- ✅ **Production Patterns** - Validation, error handling, pagination, and resource management

**Framework Repository**: [cheshire-prototype](https://github.com/halimchaibi/cheshire-prototype)  
**Application Repository**: [cheshire-blog-app](https://github.com/halimchaibi/cheshire-blog-app)

## License

This is a demonstration application for the Cheshire framework.


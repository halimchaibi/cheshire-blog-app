# Blog API - Complete Specification Documentation

This directory contains comprehensive API specifications for the Cheshire Framework Blog Application.

## 📦 Available Specifications

### 1. **postman-api-complete.json** - Enhanced Postman Collection
- **Format**: Postman Collection v2.1
- **Size**: Complete specification with 12 endpoints
- **Features**: 
  - Full descriptions for each endpoint
  - Comprehensive request examples
  - Multiple response examples (success, errors)
  - Complete parameter documentation
  - Environment variables configured

### 2. **openapi-blog-api.yaml** - OpenAPI 3.0 Specification
- **Format**: OpenAPI 3.0.3 (YAML)
- **Features**:
  - Full OpenAPI-compliant specification
  - Reusable component schemas
  - Complete request/response schemas
  - Parameter definitions
  - Error responses documented
  - Compatible with Swagger UI, Redoc, code generators

## 🚀 Quick Start

### Using with Postman

1. **Import the Collection**:
   ```
   File → Import → Choose postman-api-complete.json
   ```

2. **Set Up Environment Variables**:
   - The collection includes default variables:
     - `url`: `http://localhost:8080` (base URL)
     - `author_id`: Sample author UUID
     - `article_id`: Sample article UUID
     - `comment_id`: Sample comment UUID

3. **Start Testing**:
   - Navigate through folders: Authors → Articles → Comments → Statistics
   - Each endpoint has example requests ready to run
   - Response examples show expected outputs

### Using with Swagger UI

1. **Online Swagger Editor**:
   ```
   Visit: https://editor.swagger.io/
   File → Import File → Choose openapi-blog-api.yaml
   ```

2. **Local Swagger UI** (with Docker):
   ```bash
   docker run -p 8081:8080 \
     -e SWAGGER_JSON=/api/openapi-blog-api.yaml \
     -v $(pwd)/openapi-blog-api.yaml:/api/openapi-blog-api.yaml \
     swaggerapi/swagger-ui
   ```
   Then visit: http://localhost:8081

3. **Local Swagger UI** (with npm):
   ```bash
   npm install -g swagger-ui-watcher
   swagger-ui-watcher openapi-blog-api.yaml
   ```

### Using with Redoc

```bash
npx @redocly/cli preview-docs openapi-blog-api.yaml
```

## 📚 API Overview

### Base URL
```
http://localhost:8080/api/v1/blog
```

### Authentication
Currently no authentication required (demo mode).

### Response Format
All responses follow a consistent structure:

**Success Response**:
```json
{
  "success": true,
  "count": {
    "total_found": 116,
    "page_size": 10
  },
  "data": [ /* array of results */ ]
}
```

**Error Response**:
```json
{
  "success": false,
  "error": {
    "code": 400,
    "message": "Validation error",
    "details": "Username must be at least 3 characters long"
  }
}
```

## 🎯 Endpoints Summary

### Authors (5 endpoints)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/list_authors` | GET | List authors with pagination and filtering |
| `/create_author` | GET | Create a new author |
| `/update_author` | GET | Update author information |
| `/delete_author` | GET | Delete author (cascade deletes articles/comments) |
| `/author_details` | GET | Get detailed author information with statistics |

### Articles (3 endpoints)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/create_article` | GET | Create a new article (draft or published) |
| `/update_article` | GET | Update article content or publication status |
| `/list_articles_by_author` | GET | List articles by author with advanced filtering |

### Comments (3 endpoints)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/comment_on_article` | GET | Add a comment to an article |
| `/update_comment` | GET | Update comment content |
| `/list_comments_by_article` | GET | List comments for an article with filtering |

### Statistics (1 endpoint)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/stats_overview` | GET | Comprehensive system statistics and analytics |

## 🔍 Key Features Demonstrated

### 1. Advanced Filtering
```
GET /list_authors?search_author=john&created_after=2025-01-01&min_articles=10
```
- Wildcard search
- Date range filtering
- Numeric threshold filters

### 2. Full-Text Search
```
GET /list_articles_by_author?author_id={id}&search_content=Java
```
- PostgreSQL tsvector-based search
- Searches title and content
- Results ranked by relevance

### 3. Pagination
```
GET /list_authors?page=2&limit=50
```
- Consistent across all list endpoints
- Returns total count and page navigation metadata
- Configurable page size (1-100)

### 4. Cascade Operations
```
GET /delete_author?id={id}&confirm=true
```
- Automatic cascade delete
- Returns count of deleted related records
- Safety confirmation required

### 5. Window Functions & Aggregations
```
GET /list_authors
```
Returns statistics per author:
- Article count (total, published, drafts)
- Comment count on all articles
- Latest publication date

### 6. Comprehensive Statistics
```
GET /stats_overview?timeframe=month
```
Returns system-wide metrics:
- Author statistics
- Article statistics
- Comment statistics
- Engagement metrics
- Calculated ratios and percentages

## 🛠️ Code Generation

### Generate Client SDK

**TypeScript/JavaScript** (using openapi-generator):
```bash
npx @openapitools/openapi-generator-cli generate \
  -i openapi-blog-api.yaml \
  -g typescript-axios \
  -o ./generated/typescript-client
```

**Java** (using openapi-generator):
```bash
npx @openapitools/openapi-generator-cli generate \
  -i openapi-blog-api.yaml \
  -g java \
  -o ./generated/java-client \
  --library=okhttp-gson
```

**Python** (using openapi-generator):
```bash
npx @openapitools/openapi-generator-cli generate \
  -i openapi-blog-api.yaml \
  -g python \
  -o ./generated/python-client
```

### Generate Server Stubs

**Spring Boot** (Java):
```bash
npx @openapitools/openapi-generator-cli generate \
  -i openapi-blog-api.yaml \
  -g spring \
  -o ./generated/spring-server
```

**Express** (Node.js):
```bash
npx @openapitools/openapi-generator-cli generate \
  -i openapi-blog-api.yaml \
  -g nodejs-express-server \
  -o ./generated/express-server
```

## 📊 Demo Scenarios

### Scenario 1: Create Author and First Article
```bash
# 1. Create an author
GET /create_author?username=demo_user&email=demo@example.com

# Response: { "data": [{ "id": "uuid-here", ... }] }

# 2. Create an article
GET /create_article?author_id={uuid-from-step-1}&title=My First Post&content=Hello World!&is_published=true

# 3. Verify in author details
GET /author_details?id={uuid-from-step-1}
```

### Scenario 2: Search and Filter Content
```bash
# Find all authors who have published Java-related content
GET /list_authors?search_content=Java&has_published=true&min_articles=5

# Get their articles
GET /list_articles_by_author?author_id={uuid}&search_content=Java&published=true
```

### Scenario 3: Engagement Analysis
```bash
# Get overall system statistics
GET /stats_overview

# Find most commented articles
GET /list_articles_by_author?author_id={top-author-id}&sort=comment_count&sort_dir=desc

# View comments
GET /list_comments_by_article?article_id={article-id}
```

## 🧪 Testing

### Postman Testing
1. Import the collection
2. Run entire collection: Collection → Run
3. View test results and response examples

### API Testing with curl

**Create Author**:
```bash
curl "http://localhost:8080/api/v1/blog/create_author?username=test_user&email=test@example.com"
```

**List Authors**:
```bash
curl "http://localhost:8080/api/v1/blog/list_authors?limit=10"
```

**Get Statistics**:
```bash
curl "http://localhost:8080/api/v1/blog/stats_overview"
```

## 📝 Validation & Standards

### OpenAPI Validation
Validate the OpenAPI spec:
```bash
npx @redocly/cli lint openapi-blog-api.yaml
```

### Postman Collection Validation
```bash
npx newman collection validate postman-api-complete.json
```

## 🎨 Schema Highlights

### Reusable Components

The OpenAPI spec defines reusable schemas:
- **Author**: Basic author entity
- **AuthorWithStats**: Author with article/comment statistics
- **Article**: Article entity with publication info
- **ArticleWithDetails**: Article with preview and engagement metrics
- **Comment**: Comment entity
- **PaginationMetadata**: Consistent pagination structure
- **ErrorResponse**: Standard error format

### Parameter Components

Reusable parameters:
- `AuthorId`: UUID validation
- `SearchAuthor`: Wildcard search
- `SearchContent`: Full-text search
- `CreatedAfter/Before`: Date range filters
- `Page/Limit`: Pagination controls
- `Sort/SortDir`: Sorting options

## 📦 Integration Examples

### Postman Environment Setup
```json
{
  "name": "Blog API Local",
  "values": [
    { "key": "url", "value": "http://localhost:8080", "enabled": true },
    { "key": "author_id", "value": "", "enabled": true },
    { "key": "article_id", "value": "", "enabled": true }
  ]
}
```

### Swagger UI Docker Compose
```yaml
version: '3'
services:
  swagger-ui:
    image: swaggerapi/swagger-ui
    ports:
      - "8081:8080"
    environment:
      SWAGGER_JSON: /api/openapi-blog-api.yaml
    volumes:
      - ./openapi-blog-api.yaml:/api/openapi-blog-api.yaml
```

## 🆘 Troubleshooting

### Common Issues

**1. Server not responding**:
- Ensure the Blog App is running: `java -jar blog-app.jar --rest`
- Check server is on port 8080: `curl http://localhost:8080/api/v1/blog/stats_overview`

**2. UUID format errors**:
- All IDs must be valid UUIDs (e.g., `7a180515-2105-4e8e-9c20-0e08a694ffeb`)
- Get valid IDs from create/list operations

**3. Validation errors**:
- Check required fields are provided
- Verify field lengths and formats
- See error response `details` for specific issues

**4. Cascade delete warnings**:
- `delete_author` requires `confirm=true` parameter
- Returns count of cascade-deleted articles/comments

## 📚 Additional Resources

### Official Documentation
- [OpenAPI Specification](https://swagger.io/specification/)
- [Postman Documentation](https://learning.postman.com/docs/)
- [Cheshire Framework](../../README.md)

### Tools & Utilities
- [Swagger Editor](https://editor.swagger.io/)
- [Swagger UI](https://swagger.io/tools/swagger-ui/)
- [Redoc](https://redocly.com/redoc/)
- [OpenAPI Generator](https://openapi-generator.tech/)
- [Postman](https://www.postman.com/)

## 🎓 Demo Features Showcase

This API specification demonstrates:
✅ RESTful API design patterns
✅ Comprehensive parameter validation
✅ Advanced filtering and search
✅ Pagination with metadata
✅ Full-text search (PostgreSQL tsvector)
✅ Window functions for statistics
✅ Cascade operations
✅ Error handling
✅ OpenAPI 3.0 compliance
✅ Postman Collection v2.1 format
✅ Reusable schemas and parameters
✅ Multiple response examples
✅ Complete documentation

## 📄 License

This API specification is part of the Cheshire Framework demonstration application.

---

**Generated**: January 2026  
**Version**: 1.0.0  
**Endpoints**: 12  
**Schemas**: 20+  
**Examples**: 30+


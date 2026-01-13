# Blog Data Generation Script

This script generates realistic SQL INSERT statements for the blog database using Faker.

## Installation

### Option 1: Using the Virtual Environment (Recommended)

A Python virtual environment has been created in this directory with all required dependencies:

```bash
# Activate the virtual environment
source venv/bin/activate  # On Linux/Mac
# or
venv\Scripts\activate     # On Windows

# Install dependencies (if not already installed)
pip install -r requirements.txt
```

The virtual environment includes:
- `faker` - For generating realistic test data
- `psycopg2-binary` - For PostgreSQL database connectivity

### Option 2: Install Globally

If you prefer to install Faker globally:

```bash
pip install faker
```

### Option 3: Install from requirements.txt

```bash
pip install -r requirements.txt
```

## Usage

### Using the Virtual Environment

If using the virtual environment, activate it first:

```bash
source venv/bin/activate  # On Linux/Mac
```

Then run the script:

```bash
python generate_test_data.py
```

Or use the virtual environment's Python directly:

```bash
./venv/bin/python generate_test_data.py
```

### Basic Usage

Generate default amounts (100 authors, 500 articles, 2000 comments):

```bash
python generate_test_data.py
```

### Custom Amounts

Generate specific amounts of data:

```bash
python generate_test_data.py --authors 200 --articles 1000 --comments 5000
```

### Save to File

Save the generated SQL to a file:

```bash
python generate_test_data.py --authors 100 --articles 500 --comments 2000 --output data.sql
```

### Reproducible Generation

Use a seed for reproducible data:

```bash
python generate_test_data.py --seed 42 --output data.sql
```

### Adjust Published Ratio

Control the ratio of published vs draft articles:

```bash
python generate_test_data.py --published-ratio 0.8  # 80% published
```

## Examples

### Small Dataset (for testing)

```bash
python generate_test_data.py --authors 20 --articles 50 --comments 200 --output small_data.sql
```

### Medium Dataset (for development)

```bash
python generate_test_data.py --authors 100 --articles 500 --comments 2000 --output medium_data.sql
```

### Large Dataset (for performance testing)

```bash
python generate_test_data.py --authors 500 --articles 5000 --comments 25000 --output large_data.sql
```

### Very Large Dataset (for stress testing)

```bash
python generate_test_data.py --authors 1000 --articles 10000 --comments 50000 --output very_large_data.sql
```

## Features

- **Realistic Data**: Uses Faker to generate realistic usernames, emails, names, and content
- **Referential Integrity**: Maintains proper foreign key relationships
- **UUID Support**: Generates UUIDs for all primary keys
- **Date Distribution**: Articles and comments are distributed over the past year
- **Published/Draft Mix**: Configurable ratio of published vs draft articles
- **SQL Safe**: Properly escapes single quotes and handles NULL values

## Output Format

The script generates SQL INSERT statements in the following order:

1. **Authors**: All authors with UUIDs, usernames, emails, and creation dates
2. **Articles**: Articles referencing authors, with titles, content, publish dates, and publication status
3. **Comments**: Comments referencing articles, with author info and content

## Populating the Database

### Option 1: Using the Python Populate Script (Recommended)

The `populate_db.py` script provides an easy way to populate your PostgreSQL database:

```bash
# Activate virtual environment
source venv/bin/activate

# Populate using existing SQL file
python populate_db.py --sql-file data.sql

# Generate and populate in one step
python populate_db.py --generate --authors 100 --articles 500 --comments 2000

# Use Docker exec method (alternative)
python populate_db.py --sql-file data.sql --use-docker-exec

# Custom connection parameters
python populate_db.py --sql-file data.sql --host localhost --port 5432 --user blog_user --password blog_password
```

The script will:
- Connect to your database with retry logic
- Show progress during execution
- Display table counts before and after
- Handle errors gracefully

### Option 2: Using psql Directly

```bash
# Using psql
psql -U blog_user -d blog_db -f data.sql

# Using Docker
docker exec -i blog-db psql -U blog_user -d blog_db < data.sql
```

### Option 3: Using Docker Exec

```bash
# Copy file to container
docker cp data.sql blog-db:/tmp/data.sql

# Execute in container
docker exec -i blog-db psql -U blog_user -d blog_db -f /tmp/data.sql
```

## Notes

- The script ensures unique usernames and emails for authors
- Article content is truncated to 500 characters in the SQL for readability (full content is generated)
- Comments are distributed across all articles
- All dates are in the past (articles up to 1 year old, comments up to 6 months old)


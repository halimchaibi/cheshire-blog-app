#!/usr/bin/env python3
"""
Populate PostgreSQL database with generated SQL data.

This script can:
- Connect to PostgreSQL running in Docker or directly
- Execute SQL files to populate the database
- Show progress and handle errors gracefully

Usage:
    python populate_db.py [--sql-file FILE] [--generate] [--authors N] [--articles N] [--comments N]
    python populate_db.py --sql-file data.sql
    python populate_db.py --generate --authors 100 --articles 500 --comments 2000
"""

import argparse
import sys
import time
from pathlib import Path

try:
    import psycopg2
    from psycopg2.extensions import ISOLATION_LEVEL_AUTOCOMMIT
except ImportError:
    print("Error: psycopg2 is not installed. Install it with: pip install psycopg2-binary", file=sys.stderr)
    sys.exit(1)


class DatabasePopulator:
    """Handles database population operations."""

    def __init__(self, host='localhost', port=5432, database='blog_db', 
                 user='blog_user', password='blog_password'):
        """Initialize database connection parameters."""
        self.host = host
        self.port = port
        self.database = database
        self.user = user
        self.password = password
        self.conn = None

    def connect(self, retries=5, delay=2):
        """
        Connect to PostgreSQL database with retry logic.
        
        Args:
            retries: Number of connection retry attempts
            delay: Delay between retries in seconds
        """
        for attempt in range(1, retries + 1):
            try:
                print(f"Connecting to database (attempt {attempt}/{retries})...", file=sys.stderr)
                self.conn = psycopg2.connect(
                    host=self.host,
                    port=self.port,
                    database=self.database,
                    user=self.user,
                    password=self.password,
                    connect_timeout=5
                )
                self.conn.set_isolation_level(ISOLATION_LEVEL_AUTOCOMMIT)
                print(f"✓ Connected to database '{self.database}' at {self.host}:{self.port}", file=sys.stderr)
                return True
            except psycopg2.OperationalError as e:
                if attempt < retries:
                    print(f"Connection failed: {e}. Retrying in {delay} seconds...", file=sys.stderr)
                    time.sleep(delay)
                else:
                    print(f"❌ Failed to connect after {retries} attempts: {e}", file=sys.stderr)
                    return False
        return False

    def execute_sql_file(self, sql_file_path):
        """
        Execute SQL statements from a file.
        
        Args:
            sql_file_path: Path to SQL file
            
        Returns:
            True if successful, False otherwise
        """
        sql_file = Path(sql_file_path)
        if not sql_file.exists():
            print(f"❌ SQL file not found: {sql_file_path}", file=sys.stderr)
            return False

        print(f"Reading SQL file: {sql_file_path} ({sql_file.stat().st_size / 1024:.1f} KB)", file=sys.stderr)
        
        try:
            with open(sql_file, 'r', encoding='utf-8') as f:
                sql_content = f.read()
        except Exception as e:
            print(f"❌ Failed to read SQL file: {e}", file=sys.stderr)
            return False

        # Split SQL into individual statements
        # Remove comments and split by semicolons
        statements = []
        current_statement = []
        in_comment = False
        
        for line in sql_content.split('\n'):
            stripped = line.strip()
            
            # Skip empty lines and comment-only lines
            if not stripped or stripped.startswith('--'):
                continue
            
            # Handle multi-line comments (not common in our SQL, but handle it)
            if '/*' in stripped:
                in_comment = True
            if '*/' in stripped:
                in_comment = False
                continue
            if in_comment:
                continue
            
            current_statement.append(line)
            
            # If line ends with semicolon, it's the end of a statement
            if stripped.endswith(';'):
                statement = '\n'.join(current_statement).strip()
                if statement:
                    statements.append(statement)
                current_statement = []

        print(f"Found {len(statements)} SQL statements to execute", file=sys.stderr)
        
        cursor = self.conn.cursor()
        executed = 0
        failed = 0
        
        try:
            for i, statement in enumerate(statements, 1):
                try:
                    start_time = time.time()
                    cursor.execute(statement)
                    duration = time.time() - start_time
                    executed += 1
                    
                    # Show progress for large operations
                    if len(statements) > 10 and (i % max(1, len(statements) // 10) == 0 or i == len(statements)):
                        print(f"  Progress: {i}/{len(statements)} statements executed ({executed} successful, {failed} failed)", file=sys.stderr)
                    
                except psycopg2.Error as e:
                    failed += 1
                    # Don't fail on duplicate key errors (data might already exist)
                    if 'duplicate key' in str(e).lower() or 'already exists' in str(e).lower():
                        print(f"  ⚠ Warning (statement {i}): {e.pgcode} - {e.pgerror}", file=sys.stderr)
                    else:
                        print(f"  ❌ Error executing statement {i}: {e.pgcode} - {e.pgerror}", file=sys.stderr)
                        # Print first 200 chars of the statement for debugging
                        stmt_preview = statement[:200].replace('\n', ' ')
                        print(f"     Statement preview: {stmt_preview}...", file=sys.stderr)
                        # Continue with next statement instead of failing completely
                        continue
            
            print(f"\n✓ Execution complete: {executed} statements succeeded, {failed} failed", file=sys.stderr)
            return failed == 0
            
        except Exception as e:
            print(f"❌ Unexpected error during execution: {e}", file=sys.stderr)
            return False
        finally:
            cursor.close()

    def get_table_counts(self):
        """Get row counts from all tables."""
        cursor = self.conn.cursor()
        try:
            counts = {}
            tables = ['Authors', 'Articles', 'Comments']
            for table in tables:
                cursor.execute(f"SELECT COUNT(*) FROM {table}")
                counts[table] = cursor.fetchone()[0]
            return counts
        except Exception as e:
            print(f"⚠ Warning: Could not get table counts: {e}", file=sys.stderr)
            return {}
        finally:
            cursor.close()

    def close(self):
        """Close database connection."""
        if self.conn:
            self.conn.close()
            print("Database connection closed", file=sys.stderr)


def main():
    parser = argparse.ArgumentParser(
        description="Populate PostgreSQL database with generated SQL data",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__
    )
    
    # Connection options
    parser.add_argument(
        "--host",
        type=str,
        default="localhost",
        help="Database host (default: localhost)"
    )
    parser.add_argument(
        "--port",
        type=int,
        default=5432,
        help="Database port (default: 5432)"
    )
    parser.add_argument(
        "--database",
        type=str,
        default="blog_db",
        help="Database name (default: blog_db)"
    )
    parser.add_argument(
        "--user",
        type=str,
        default="blog_user",
        help="Database user (default: blog_user)"
    )
    parser.add_argument(
        "--password",
        type=str,
        default="blog_password",
        help="Database password (default: blog_password)"
    )
    
    # SQL file options
    parser.add_argument(
        "--sql-file",
        type=str,
        default="data.sql",
        help="Path to SQL file to execute (default: data.sql)"
    )
    
    # Generation options
    parser.add_argument(
        "--generate",
        action="store_true",
        help="Generate SQL data before populating (requires generate_test_data.py)"
    )
    parser.add_argument(
        "--authors",
        type=int,
        default=100,
        help="Number of authors to generate (default: 100, only with --generate)"
    )
    parser.add_argument(
        "--articles",
        type=int,
        default=500,
        help="Number of articles to generate (default: 500, only with --generate)"
    )
    parser.add_argument(
        "--comments",
        type=int,
        default=2000,
        help="Number of comments to generate (default: 2000, only with --generate)"
    )
    
    # Docker options
    parser.add_argument(
        "--docker-container",
        type=str,
        default="blog-db",
        help="Docker container name (if using Docker exec method)"
    )
    parser.add_argument(
        "--use-docker-exec",
        action="store_true",
        help="Use docker exec instead of direct connection (alternative method)"
    )
    
    args = parser.parse_args()
    
    # Generate SQL if requested
    sql_file = args.sql_file
    if args.generate:
        print("Generating SQL data...", file=sys.stderr)
        import subprocess
        import tempfile
        
        if args.use_docker_exec:
            # Generate to temp file
            temp_file = tempfile.NamedTemporaryFile(mode='w', suffix='.sql', delete=False)
            sql_file = temp_file.name
            temp_file.close()
        
        gen_cmd = [
            sys.executable,
            "generate_test_data.py",
            "--authors", str(args.authors),
            "--articles", str(args.articles),
            "--comments", str(args.comments),
            "--output", sql_file
        ]
        
        result = subprocess.run(gen_cmd, capture_output=True, text=True)
        if result.returncode != 0:
            print(f"❌ Failed to generate SQL: {result.stderr}", file=sys.stderr)
            sys.exit(1)
        print(f"✓ SQL data generated: {sql_file}", file=sys.stderr)
    
    # Use Docker exec method if requested
    if args.use_docker_exec:
        print(f"Using Docker exec method with container: {args.docker_container}", file=sys.stderr)
        import subprocess
        
        # Check if container exists
        check_cmd = ["docker", "ps", "-a", "--filter", f"name={args.docker_container}", "--format", "{{.Names}}"]
        result = subprocess.run(check_cmd, capture_output=True, text=True)
        if args.docker_container not in result.stdout:
            print(f"❌ Docker container '{args.docker_container}' not found", file=sys.stderr)
            sys.exit(1)
        
        # Copy SQL file to container and execute
        print(f"Copying SQL file to container...", file=sys.stderr)
        copy_cmd = ["docker", "cp", sql_file, f"{args.docker_container}:/tmp/data.sql"]
        result = subprocess.run(copy_cmd)
        if result.returncode != 0:
            print(f"❌ Failed to copy SQL file to container", file=sys.stderr)
            sys.exit(1)
        
        print("Executing SQL in container...", file=sys.stderr)
        exec_cmd = [
            "docker", "exec", "-i", args.docker_container,
            "psql", "-U", args.user, "-d", args.database, "-f", "/tmp/data.sql"
        ]
        result = subprocess.run(exec_cmd, env={"PGPASSWORD": args.password})
        
        if result.returncode == 0:
            print("✓ Database populated successfully via Docker exec", file=sys.stderr)
        else:
            print("❌ Failed to populate database via Docker exec", file=sys.stderr)
            sys.exit(1)
        
        return
    
    # Direct connection method
    populator = DatabasePopulator(
        host=args.host,
        port=args.port,
        database=args.database,
        user=args.user,
        password=args.password
    )
    
    try:
        if not populator.connect():
            sys.exit(1)
        
        # Get initial counts
        print("\nCurrent database state:", file=sys.stderr)
        initial_counts = populator.get_table_counts()
        for table, count in initial_counts.items():
            print(f"  {table}: {count} rows", file=sys.stderr)
        
        # Execute SQL file
        print(f"\nExecuting SQL file: {sql_file}", file=sys.stderr)
        if not populator.execute_sql_file(sql_file):
            print("❌ Failed to populate database", file=sys.stderr)
            sys.exit(1)
        
        # Get final counts
        print("\nFinal database state:", file=sys.stderr)
        final_counts = populator.get_table_counts()
        for table, count in final_counts.items():
            initial = initial_counts.get(table, 0)
            added = count - initial
            print(f"  {table}: {count} rows (+{added})", file=sys.stderr)
        
        print("\n✅ Database populated successfully!", file=sys.stderr)
        
    except KeyboardInterrupt:
        print("\n⚠ Operation cancelled by user", file=sys.stderr)
        sys.exit(1)
    except Exception as e:
        print(f"❌ Unexpected error: {e}", file=sys.stderr)
        sys.exit(1)
    finally:
        populator.close()


if __name__ == "__main__":
    main()


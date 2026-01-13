#!/usr/bin/env python3
"""
Verify that the generated SQL maintains referential integrity.
Checks that:
- All articles reference existing authors
- All comments reference existing articles
"""

import re
import sys

def extract_uuids_from_sql(sql_file):
    """Extract all UUIDs from the SQL file and verify relationships."""
    with open(sql_file, 'r') as f:
        content = f.read()
    
    # Extract author UUIDs
    author_pattern = r"INSERT INTO Authors.*?VALUES\s+(.*?);"
    author_match = re.search(author_pattern, content, re.DOTALL)
    author_ids = set()
    
    if author_match:
        author_values = author_match.group(1)
        for match in re.finditer(r"\('([a-f0-9-]{36})'", author_values):
            author_ids.add(match.group(1))
    
    print(f"Found {len(author_ids)} authors")
    
    # Extract article UUIDs and their author references
    article_pattern = r"INSERT INTO Articles.*?VALUES\s+(.*?);"
    article_match = re.search(article_pattern, content, re.DOTALL)
    article_ids = set()
    article_author_refs = []
    
    if article_match:
        article_values = article_match.group(1)
        # Pattern: ('article_uuid', 'title', 'content', publish_date, is_published, 'author_id', ...)
        # We need to find the 6th field which is the author_id
        for line in article_values.split('\n'):
            if line.strip().startswith('('):
                # Extract article UUID (1st field)
                uuid_match = re.search(r"\('([a-f0-9-]{36})'", line)
                if uuid_match:
                    article_id = uuid_match.group(1)
                    article_ids.add(article_id)
                    
                    # Extract author_id (6th field - after 5 commas)
                    # This is a simplified approach - in practice, we'd need proper SQL parsing
                    parts = line.split(',')
                    if len(parts) >= 6:
                        # The author_id is the 6th element (index 5), but we need to extract UUID from it
                        author_ref = parts[5].strip()
                        author_uuid_match = re.search(r"'([a-f0-9-]{36})'", author_ref)
                        if author_uuid_match:
                            article_author_refs.append((article_id, author_uuid_match.group(1)))
    
    print(f"Found {len(article_ids)} articles")
    
    # Verify article->author relationships
    invalid_article_refs = []
    for article_id, author_id in article_author_refs:
        if author_id not in author_ids:
            invalid_article_refs.append((article_id, author_id))
    
    if invalid_article_refs:
        print(f"\n❌ ERROR: Found {len(invalid_article_refs)} articles referencing non-existent authors!")
        for article_id, author_id in invalid_article_refs[:5]:
            print(f"   Article {article_id[:8]}... references author {author_id[:8]}... (NOT FOUND)")
        return False
    else:
        print(f"✓ All {len(article_author_refs)} articles reference valid authors")
    
    # Extract comment UUIDs and their article references
    comment_pattern = r"INSERT INTO Comments.*?VALUES\s+(.*?);"
    comment_match = re.search(comment_pattern, content, re.DOTALL)
    comment_article_refs = []
    
    if comment_match:
        comment_values = comment_match.group(1)
        for line in comment_values.split('\n'):
            if line.strip().startswith('('):
                # Extract comment UUID and article_id (2nd field)
                uuid_match = re.search(r"\('([a-f0-9-]{36})'", line)
                if uuid_match:
                    comment_id = uuid_match.group(1)
                    # Extract article_id (2nd field - after first comma)
                    parts = line.split(',')
                    if len(parts) >= 2:
                        article_ref = parts[1].strip()
                        article_uuid_match = re.search(r"'([a-f0-9-]{36})'", article_ref)
                        if article_uuid_match:
                            comment_article_refs.append((comment_id, article_uuid_match.group(1)))
    
    print(f"Found {len(comment_article_refs)} comments")
    
    # Verify comment->article relationships
    invalid_comment_refs = []
    for comment_id, article_id in comment_article_refs:
        if article_id not in article_ids:
            invalid_comment_refs.append((comment_id, article_id))
    
    if invalid_comment_refs:
        print(f"\n❌ ERROR: Found {len(invalid_comment_refs)} comments referencing non-existent articles!")
        for comment_id, article_id in invalid_comment_refs[:5]:
            print(f"   Comment {comment_id[:8]}... references article {article_id[:8]}... (NOT FOUND)")
        return False
    else:
        print(f"✓ All {len(comment_article_refs)} comments reference valid articles")
    
    print("\n✅ All referential integrity checks passed!")
    return True

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python verify_relationships.py <sql_file>")
        sys.exit(1)
    
    sql_file = sys.argv[1]
    success = extract_uuids_from_sql(sql_file)
    sys.exit(0 if success else 1)


#!/usr/bin/env python3
"""
Generate SQL INSERT statements for blog database using Faker.

Usage:
    python generate_test_data.py [--authors N] [--articles N] [--comments N] [--output FILE]

Examples:
    python generate_test_data.py --authors 100 --articles 500 --comments 2000
    python generate_test_data.py --authors 50 --articles 200 --comments 1000 --output data.sql
"""

import argparse
import sys
from datetime import datetime, timedelta
from random import choice, randint, random
from typing import List, Tuple
from uuid import uuid4

try:
    from faker import Faker
except ImportError:
    print("Error: Faker is not installed. Install it with: pip install faker", file=sys.stderr)
    sys.exit(1)


class BlogDataGenerator:
    """Generates realistic blog data using Faker."""

    def __init__(self, seed: int = None):
        """Initialize Faker with optional seed for reproducibility."""
        self.fake = Faker()
        if seed is not None:
            Faker.seed(seed)
            self.fake.seed_instance(seed)

    def generate_authors(self, count: int) -> List[Tuple[str, str, str, str]]:
        """
        Generate author data.
        
        Returns: List of (uuid, username, email, created_at) tuples
        """
        authors = []
        usernames = set()
        
        for _ in range(count):
            # Generate unique username
            username = None
            attempts = 0
            while username is None or username in usernames:
                if attempts > 100:
                    # Fallback to UUID-based username if too many collisions
                    username = f"user_{uuid4().hex[:8]}"
                    break
                username = self.fake.user_name().lower().replace(' ', '_')
                attempts += 1
            
            usernames.add(username)
            email = self.fake.unique.email()
            uuid_str = str(uuid4())
            created_at = self.fake.date_time_between(start_date='-2y', end_date='now')
            created_at_str = created_at.strftime('%Y-%m-%d %H:%M:%S')
            
            authors.append((uuid_str, username, email, created_at_str))
        
        return authors

    def generate_articles(
        self, 
        count: int, 
        author_ids: List[str],
        published_ratio: float = 0.7
    ) -> List[Tuple[str, str, str, str, str, str, str]]:
        """
        Generate article data.
        
        Args:
            count: Number of articles to generate
            author_ids: List of author UUIDs to reference
            published_ratio: Ratio of published articles (0.0 to 1.0)
        
        Returns: List of (uuid, title, content, publish_date, is_published, author_id, created_at) tuples
        """
        articles = []
        
        # Article topics for variety
        topics = [
            "Technology", "Programming", "Web Development", "Data Science",
            "Machine Learning", "DevOps", "Cloud Computing", "Security",
            "Mobile Development", "Database Design", "API Development",
            "Frontend", "Backend", "Full Stack", "Software Architecture",
            "Testing", "Agile", "Open Source", "Career", "Tutorial"
        ]
        
        for _ in range(count):
            uuid_str = str(uuid4())
            author_id = choice(author_ids)
            
            # Generate title
            topic = choice(topics)
            title_patterns = [
                f"Getting Started with {topic}",
                f"Advanced {topic} Techniques",
                f"{topic} Best Practices",
                f"Introduction to {topic}",
                f"{topic} Tutorial: A Complete Guide",
                f"Understanding {topic}",
                f"{topic} Tips and Tricks",
                f"Mastering {topic}",
                f"{topic} Fundamentals",
                f"Exploring {topic}",
            ]
            title = choice(title_patterns)
            
            # Generate content (paragraphs of text)
            num_paragraphs = randint(3, 8)
            content = "\n\n".join(self.fake.paragraphs(nb=num_paragraphs))
            # Escape single quotes for SQL
            content = content.replace("'", "''")
            
            # Determine if published
            is_published = random() < published_ratio
            
            # Generate dates
            days_ago = randint(0, 365)  # Articles from last year
            created_at = datetime.now() - timedelta(days=days_ago)
            
            if is_published:
                # Published articles: publish_date is same or slightly after created_at
                publish_days_ago = randint(0, days_ago)
                publish_date = datetime.now() - timedelta(days=publish_days_ago)
                publish_date_str = f"'{publish_date.strftime('%Y-%m-%d %H:%M:%S')}'"
            else:
                # Draft articles: no publish_date
                publish_date_str = "NULL"
            
            created_at_str = f"'{created_at.strftime('%Y-%m-%d %H:%M:%S')}'"
            
            articles.append((
                uuid_str,
                title.replace("'", "''"),  # Escape single quotes
                content,
                publish_date_str,
                str(is_published).upper(),
                author_id,
                created_at_str
            ))
        
        return articles

    def generate_comments(
        self,
        count: int,
        article_ids: List[str]
    ) -> List[Tuple[str, str, str, str, str, str, str]]:
        """
        Generate comment data.
        
        Args:
            count: Number of comments to generate
            article_ids: List of article UUIDs to reference
        
        Returns: List of (uuid, article_id, author_name, author_email, content, comment_date, created_at) tuples
        """
        comments = []
        
        # Comment templates for variety
        comment_templates = [
            "Great article! Very helpful.",
            "Thanks for sharing this.",
            "I found this really useful.",
            "Excellent explanation!",
            "This helped me understand the concept better.",
            "Nice write-up!",
            "I have a question about...",
            "Could you elaborate on...?",
            "I disagree with...",
            "This is exactly what I was looking for!",
            "Well written and informative.",
            "I'll definitely try this approach.",
            "Thanks for the detailed explanation.",
            "This cleared up my confusion.",
            "Great examples in this article.",
        ]
        
        for _ in range(count):
            uuid_str = str(uuid4())
            article_id = choice(article_ids)
            
            # Generate commenter info
            author_name = self.fake.name()
            author_email = self.fake.email()
            
            # Generate content (mix of templates and custom)
            if random() < 0.3:
                # 30% chance of using template
                content = choice(comment_templates)
            else:
                # 70% chance of generating custom comment
                content = self.fake.sentence(nb_words=randint(5, 20))
            
            # Escape single quotes
            content = content.replace("'", "''")
            
            # Generate dates (comments are usually recent, but can be older)
            days_ago = randint(0, 180)  # Comments from last 6 months
            comment_date = datetime.now() - timedelta(days=days_ago)
            created_at = comment_date  # Usually same as comment_date
            
            comment_date_str = f"'{comment_date.strftime('%Y-%m-%d %H:%M:%S')}'"
            created_at_str = f"'{created_at.strftime('%Y-%m-%d %H:%M:%S')}'"
            
            comments.append((
                uuid_str,
                article_id,
                author_name.replace("'", "''"),
                author_email,
                content,
                comment_date_str,
                created_at_str
            ))
        
        return comments

    def generate_sql(
        self,
        authors: List[Tuple[str, str, str, str]],
        articles: List[Tuple[str, str, str, str, str, str, str]],
        comments: List[Tuple[str, str, str, str, str, str, str]]
    ) -> str:
        """Generate SQL INSERT statements."""
        sql_lines = []
        
        # Header comment
        sql_lines.append("-- Generated SQL INSERT statements")
        sql_lines.append(f"-- Generated at: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        sql_lines.append(f"-- Authors: {len(authors)}, Articles: {len(articles)}, Comments: {len(comments)}")
        sql_lines.append("")
        
        # Insert Authors
        sql_lines.append("-- Insert Authors")
        sql_lines.append("INSERT INTO Authors (id, username, email, created_at) VALUES")
        author_values = []
        for uuid_str, username, email, created_at in authors:
            author_values.append(
                f"('{uuid_str}', '{username}', '{email}', '{created_at}')"
            )
        sql_lines.append(",\n".join(author_values) + ";")
        sql_lines.append("")
        
        # Insert Articles
        sql_lines.append("-- Insert Articles")
        sql_lines.append("INSERT INTO Articles (id, title, content, publish_date, is_published, author_id, created_at, updated_at) VALUES")
        article_values = []
        for uuid_str, title, content, publish_date, is_published, author_id, created_at in articles:
            article_values.append(
                f"('{uuid_str}', '{title}', '{content}', {publish_date}, {is_published}, '{author_id}', {created_at}, {created_at})"
            )
        sql_lines.append(",\n".join(article_values) + ";")
        sql_lines.append("")
        
        # Insert Comments
        sql_lines.append("-- Insert Comments")
        sql_lines.append("INSERT INTO Comments (id, article_id, author_name, author_email, content, comment_date, created_at) VALUES")
        comment_values = []
        for uuid_str, article_id, author_name, author_email, content, comment_date, created_at in comments:
            comment_values.append(
                f"('{uuid_str}', '{article_id}', '{author_name}', '{author_email}', '{content}', {comment_date}, {created_at})"
            )
        sql_lines.append(",\n".join(comment_values) + ";")
        sql_lines.append("")
        
        # Summary
        sql_lines.append("-- Summary")
        sql_lines.append("SELECT")
        sql_lines.append("    'Data Generation Complete' AS message,")
        sql_lines.append(f"    {len(authors)} AS total_authors,")
        sql_lines.append(f"    {len(articles)} AS total_articles,")
        sql_lines.append(f"    {len(comments)} AS total_comments;")
        
        return "\n".join(sql_lines)


def main():
    parser = argparse.ArgumentParser(
        description="Generate SQL INSERT statements for blog database",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__
    )
    parser.add_argument(
        "--authors",
        type=int,
        default=100,
        help="Number of authors to generate (default: 100)"
    )
    parser.add_argument(
        "--articles",
        type=int,
        default=500,
        help="Number of articles to generate (default: 500)"
    )
    parser.add_argument(
        "--comments",
        type=int,
        default=2000,
        help="Number of comments to generate (default: 2000)"
    )
    parser.add_argument(
        "--output",
        type=str,
        default=None,
        help="Output file path (default: stdout)"
    )
    parser.add_argument(
        "--seed",
        type=int,
        default=None,
        help="Random seed for reproducibility"
    )
    parser.add_argument(
        "--published-ratio",
        type=float,
        default=0.7,
        help="Ratio of published articles (0.0 to 1.0, default: 0.7)"
    )
    
    args = parser.parse_args()
    
    # Validate arguments
    if args.authors < 1:
        print("Error: --authors must be at least 1", file=sys.stderr)
        sys.exit(1)
    if args.articles < 1:
        print("Error: --articles must be at least 1", file=sys.stderr)
        sys.exit(1)
    if args.comments < 1:
        print("Error: --comments must be at least 1", file=sys.stderr)
        sys.exit(1)
    if not 0.0 <= args.published_ratio <= 1.0:
        print("Error: --published-ratio must be between 0.0 and 1.0", file=sys.stderr)
        sys.exit(1)
    
    print(f"Generating data: {args.authors} authors, {args.articles} articles, {args.comments} comments...", file=sys.stderr)
    
    # Generate data
    generator = BlogDataGenerator(seed=args.seed)
    
    print("Generating authors...", file=sys.stderr)
    authors = generator.generate_authors(args.authors)
    
    print("Generating articles...", file=sys.stderr)
    author_ids = [a[0] for a in authors]
    articles = generator.generate_articles(args.articles, author_ids, args.published_ratio)
    
    print("Generating comments...", file=sys.stderr)
    article_ids = [a[0] for a in articles]
    comments = generator.generate_comments(args.comments, article_ids)
    
    print("Generating SQL...", file=sys.stderr)
    sql = generator.generate_sql(authors, articles, comments)
    
    # Write output
    if args.output:
        with open(args.output, 'w', encoding='utf-8') as f:
            f.write(sql)
        print(f"SQL written to: {args.output}", file=sys.stderr)
    else:
        print(sql)


if __name__ == "__main__":
    main()


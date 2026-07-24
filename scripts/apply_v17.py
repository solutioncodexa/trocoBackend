"""Apply V17 drop product types and collections to Neon."""
import re
from pathlib import Path
import psycopg2

props = Path(r"c:\Users\DELL\Desktop\troco\troco backend\troco-dev-db.properties").read_text(encoding="utf-8")
pwd = re.search(r"troco\.dev\.db\.password=(.+)", props).group(1).strip()
sql = Path(
    r"c:\Users\DELL\Desktop\troco\troco backend\src\main\resources\db\migration\V17__drop_types_and_collections.sql"
).read_text(encoding="utf-8")

conn = psycopg2.connect(
    host="ep-steep-sea-a2yj95ci-pooler.eu-central-1.aws.neon.tech",
    dbname="neondb",
    user="neondb_owner",
    password=pwd,
    sslmode="require",
)
conn.autocommit = True
cur = conn.cursor()
cur.execute(sql)
print("V17 applied")
cur.execute(
    """
    SELECT column_name
    FROM information_schema.columns
    WHERE table_name = 'products'
      AND column_name IN ('product_type', 'collection')
    ORDER BY column_name
    """
)
print("remaining product columns (should be empty):", cur.fetchall())
cur.execute(
    """
    SELECT table_name
    FROM information_schema.tables
    WHERE table_schema = 'public'
      AND table_name IN ('product_types', 'collections')
    ORDER BY table_name
    """
)
print("remaining tables (should be empty):", cur.fetchall())
cur.close()
conn.close()

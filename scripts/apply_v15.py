"""Apply V15 home_hero_settings schema to Neon."""
import re
from pathlib import Path
import psycopg2

props = Path(r"c:\Users\DELL\Desktop\troco\troco backend\troco-dev-db.properties").read_text(encoding="utf-8")
pwd = re.search(r"troco\.dev\.db\.password=(.+)", props).group(1).strip()
sql = Path(
    r"c:\Users\DELL\Desktop\troco\troco backend\src\main\resources\db\migration\V15__home_hero_settings.sql"
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
print("V15 applied")

cur.execute(
    """
    INSERT INTO flyway_schema_history (
      installed_rank, version, description, type, script, checksum,
      installed_by, installed_on, execution_time, success
    )
    SELECT COALESCE(MAX(installed_rank), 0) + 1, '15', 'home hero settings', 'SQL',
           'V15__home_hero_settings.sql', NULL, current_user, now(), 0, true
    FROM flyway_schema_history
    WHERE NOT EXISTS (SELECT 1 FROM flyway_schema_history WHERE version = '15')
    """
)

cur.execute("SELECT id, left(image_url, 60) FROM home_hero_settings")
print("row:", cur.fetchall())
cur.execute("SELECT version FROM flyway_schema_history WHERE version = '15'")
print("flyway:", cur.fetchall())
cur.close()
conn.close()

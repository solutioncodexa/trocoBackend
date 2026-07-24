"""Apply V16 home hero multiple images to Neon."""
import re
from pathlib import Path
import psycopg2

props = Path(r"c:\Users\DELL\Desktop\troco\troco backend\troco-dev-db.properties").read_text(encoding="utf-8")
pwd = re.search(r"troco\.dev\.db\.password=(.+)", props).group(1).strip()
sql = Path(
    r"c:\Users\DELL\Desktop\troco\troco backend\src\main\resources\db\migration\V16__home_hero_multiple_images.sql"
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
print("V16 applied")
cur.execute("SELECT id, left(image_url, 40), left(image_urls, 120) FROM home_hero_settings")
print("row:", cur.fetchall())
cur.close()
conn.close()

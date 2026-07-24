import re
from pathlib import Path
import psycopg2
import bcrypt

props = Path(r"c:\Users\DELL\Desktop\troco\troco backend\troco-dev-db.properties").read_text(encoding="utf-8")
pwd = re.search(r"troco\.dev\.db\.password=(.+)", props).group(1).strip()
conn = psycopg2.connect(
    host="ep-steep-sea-a2yj95ci-pooler.eu-central-1.aws.neon.tech",
    dbname="neondb",
    user="neondb_owner",
    password=pwd,
    sslmode="require",
)
cur = conn.cursor()
cur.execute("SELECT password FROM users WHERE email=%s", ("admin@troco.ma",))
row = cur.fetchone()
cur.close()
conn.close()
if not row:
    print("NO_ADMIN_USER")
else:
    stored = row[0].encode("utf-8")
    for candidate in [b"Admin1234", b"admin1234", b"Admin123", b"Troco1234"]:
        print(candidate.decode(), "=>", bcrypt.checkpw(candidate, stored))

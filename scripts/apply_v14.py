"""Apply V14 members/permissions/audit schema to Neon."""
import re
from pathlib import Path
import psycopg2

props = Path(r"c:\Users\DELL\Desktop\troco\troco backend\troco-dev-db.properties").read_text(encoding="utf-8")
pwd = re.search(r"troco\.dev\.db\.password=(.+)", props).group(1).strip()
sql = Path(
    r"c:\Users\DELL\Desktop\troco\troco backend\src\main\resources\db\migration\V14__members_permissions_audit.sql"
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
print("V14 applied")
cur.execute("SELECT count(*) FROM permissions")
print("permissions:", cur.fetchone()[0])
cur.execute("""
SELECT column_name FROM information_schema.columns
WHERE table_name='users' AND column_name IN ('full_name','active')
ORDER BY 1
""")
print("user cols:", cur.fetchall())
cur.execute("SELECT to_regclass('public.audit_logs')")
print("audit_logs:", cur.fetchone()[0])
cur.close()
conn.close()

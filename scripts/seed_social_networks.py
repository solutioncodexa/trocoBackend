import re
from pathlib import Path

try:
    import psycopg2
except ImportError:
    import subprocess, sys
    subprocess.check_call([sys.executable, "-m", "pip", "install", "psycopg2-binary", "-q"])
    import psycopg2

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
cur.execute("SELECT to_regclass('public.social_networks')")
print("table:", cur.fetchone()[0])
cur.execute("SELECT count(*) FROM social_networks")
print("count before:", cur.fetchone()[0])
cur.execute(
    """
INSERT INTO social_networks (network_key, label, url, enabled, display_order, updated_at) VALUES
    ('facebook',  'Facebook',  'https://www.facebook.com/profile.php?id=61589818832364', TRUE, 1, NOW()),
    ('instagram', 'Instagram', 'https://www.instagram.com/troco/', TRUE, 2, NOW()),
    ('tiktok',    'TikTok',    'https://www.tiktok.com/@troco1', TRUE, 3, NOW()),
    ('whatsapp',  'WhatsApp',  'https://wa.me/212684490098', TRUE, 4, NOW())
ON CONFLICT (network_key) DO NOTHING
"""
)
conn.commit()
cur.execute("SELECT network_key, enabled, url FROM social_networks ORDER BY display_order")
for row in cur.fetchall():
    print(row)
cur.close()
conn.close()
print("done")

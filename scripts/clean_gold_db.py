"""Drop gold tables and replace jewelry catalog with packaging seeds."""
import re
from pathlib import Path
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
conn.autocommit = False
cur = conn.cursor()

try:
    # Null legacy gold columns
    cur.execute("UPDATE products SET gold_type = NULL WHERE gold_type IS NOT NULL")
    print("products.gold_type cleared:", cur.rowcount)
    cur.execute("UPDATE cart_items SET selected_gold_type = NULL WHERE selected_gold_type IS NOT NULL")
    print("cart_items.selected_gold_type cleared:", cur.rowcount)
    cur.execute("UPDATE order_items SET selected_gold_type = NULL WHERE selected_gold_type IS NOT NULL")
    print("order_items.selected_gold_type cleared:", cur.rowcount)

    # Drop gold entity tables
    cur.execute("DROP TABLE IF EXISTS gold_price_settings CASCADE")
    cur.execute("DROP TABLE IF EXISTS gold_types CASCADE")
    print("dropped gold_price_settings + gold_types")

    # Replace jewelry product types with packaging
    cur.execute("DELETE FROM product_types")
    cur.executemany(
        """
        INSERT INTO product_types (name, code, requires_size, size_options, sort_order)
        VALUES (%s, %s, %s, %s, %s)
        """,
        [
            ("Sachet", "SACHET", False, None, 1),
            ("Carton", "CARTON", False, None, 2),
            ("Protection", "PROTECTION", False, None, 3),
            ("Décoration", "DECORATION", False, None, 4),
            ("Matériel", "MATERIEL", False, None, 5),
        ],
    )
    print("product_types reseeding done")

    # Replace jewelry collections with packaging
    cur.execute("DELETE FROM collections")
    cur.executemany(
        """
        INSERT INTO collections (name, slug, description, is_active, created_at)
        VALUES (%s, %s, %s, TRUE, NOW())
        """,
        [
            ("E-commerce", "e-commerce", "Emballages pour boutiques en ligne"),
            ("Retail", "retail", "Solutions pour points de vente"),
            ("Sur-mesure", "sur-mesure", "Créations personnalisées logo & format"),
        ],
    )
    print("collections reseeding done")

    conn.commit()
    print("COMMIT OK")
except Exception:
    conn.rollback()
    raise
finally:
    cur.execute(
        """
        SELECT table_name FROM information_schema.tables
        WHERE table_schema='public' AND table_name ILIKE '%gold%'
        ORDER BY 1
        """
    )
    print("gold tables after:", cur.fetchall())
    cur.execute("SELECT id, name, code FROM product_types ORDER BY sort_order, id")
    print("product_types:", cur.fetchall())
    cur.execute("SELECT id, name, slug FROM collections ORDER BY id")
    print("collections:", cur.fetchall())
    cur.close()
    conn.close()

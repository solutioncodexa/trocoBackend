"""Resync WooCommerce variation attributes + prices into Neon (Quantité + Taille)."""
import json
import re
import urllib.request
from pathlib import Path
from urllib.parse import unquote

import psycopg2

ROOT = Path(__file__).resolve().parents[1]
props = (ROOT / "troco-dev-db.properties").read_text(encoding="utf-8")
pwd = re.search(r"troco\.dev\.db\.password=(.+)", props).group(1).strip()
HOST = "ep-steep-sea-a2yj95ci-pooler.eu-central-1.aws.neon.tech"


def get_json(url: str):
    with urllib.request.urlopen(url, timeout=25) as r:
        return json.loads(r.read().decode())


def norm(v: str) -> str:
    return (
        unquote(v or "")
        .replace("×", "x")
        .replace("&#215;", "x")
        .replace("&times;", "x")
        .replace("\u00d7", "x")
        .strip()
    )


def clean_attr_name(name: str) -> str:
    n = (name or "Option").strip()
    nl = n.lower().replace("é", "e").replace("è", "e")
    if "quantit" in nl:
        return "Quantité"
    if "taille" in nl or "dimension" in nl:
        return "Taille"
    return n


def extract_attrs(v_list, full):
    pairs = []
    attrs = v_list.get("attributes") or full.get("attributes") or []
    for a in attrs:
        name = clean_attr_name(a.get("name") or "Option")
        value = a.get("value") or a.get("term_name")
        if value:
            pairs.append({"name": name, "value": norm(value)})
    if not pairs:
        variation = full.get("variation") or ""
        for part in variation.split(","):
            if ":" in part:
                n, val = part.split(":", 1)
                pairs.append({"name": clean_attr_name(n), "value": norm(val)})
    if not pairs:
        perm = full.get("permalink") or ""
        if "attribute_taille=" in perm:
            val = perm.split("attribute_taille=")[1].split("&")[0]
            pairs.append({"name": "Taille", "value": norm(val)})
    return pairs


def primary(pairs):
    for p in pairs:
        if "taille" in p["name"].lower():
            return p["name"], p["value"]
    if pairs:
        return pairs[0]["name"], pairs[0]["value"]
    return "Option", "Standard"


def main():
    conn = psycopg2.connect(
        host=HOST, dbname="neondb", user="neondb_owner", password=pwd, sslmode="require"
    )
    cur = conn.cursor()
    cur.execute(
        "SELECT id, external_woo_id, name FROM products WHERE external_woo_id IS NOT NULL AND deleted = false"
    )
    products = cur.fetchall()
    # ensure column exists
    cur.execute(
        "ALTER TABLE product_variants ADD COLUMN IF NOT EXISTS attributes_json TEXT"
    )
    conn.commit()

    for pid, woo_id, name in products:
        try:
            sp = get_json(f"https://troco.ma/wp-json/wc/store/v1/products/{woo_id}")
        except Exception as e:
            print("skip", woo_id, e)
            continue
        variations = sp.get("variations") or []
        if not variations:
            continue

        cur.execute("DELETE FROM product_variants WHERE product_id = %s", (pid,))
        for i, v in enumerate(variations):
            vid = v.get("id")
            try:
                full = get_json(f"https://troco.ma/wp-json/wc/store/v1/products/{vid}")
            except Exception:
                continue
            pairs = extract_attrs(v, full)
            if not pairs:
                continue
            price = float((full.get("prices") or {}).get("price") or 0) / 100.0
            if price <= 0:
                continue
            attr_name, attr_value = primary(pairs)
            label = " · ".join(p["value"] for p in pairs)
            cur.execute(
                """
                INSERT INTO product_variants
                  (product_id, attribute_name, attribute_value, attributes_json, label, price, stock, display_order, is_default)
                VALUES (%s,%s,%s,%s,%s,%s,100,%s,%s)
                """,
                (
                    pid,
                    attr_name,
                    attr_value,
                    json.dumps(pairs, ensure_ascii=False),
                    label,
                    price,
                    i,
                    i == 0,
                ),
            )
        cur.execute("SELECT MIN(price) FROM product_variants WHERE product_id=%s", (pid,))
        mn = cur.fetchone()[0]
        if mn:
            cur.execute("UPDATE products SET price=%s WHERE id=%s", (mn, pid))
        print(f"OK {pid} {name[:50]} vars={len(variations)}")

    conn.commit()
    cur.execute(
        "SELECT attribute_name, attribute_value, attributes_json, price FROM product_variants WHERE product_id=29 ORDER BY display_order LIMIT 5"
    )
    print("product 29 sample:", cur.fetchall())
    cur.close()
    conn.close()


if __name__ == "__main__":
    main()

/**
 * Seed 3 test boutiques for plan limit edge cases:
 * - basic@yopmail.com  : Basic, 49 products, 99 orders (current month)
 * - pro@yopmail.com    : Pro,   99 products, 999 orders
 * - business@yopmail.com : Business, 5 products, 5 orders (baseline)
 *
 * Usage: node scripts/seed-plan-test-tenants.mjs
 */
import pg from "pg";
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const API = process.env.API_BASE || "http://127.0.0.1:8080/api";
const PASSWORD = process.env.TEST_PASSWORD || "Test1234!";

const TENANTS = [
  { key: "basic", name: "Boutique Basic QA", slug: "basic", email: "basic@yopmail.com", plan: "basic", products: 49, orders: 99 },
  { key: "pro", name: "Boutique Pro QA", slug: "pro", email: "pro@yopmail.com", plan: "pro", products: 99, orders: 999 },
  { key: "business", name: "Boutique Business QA", slug: "business", email: "business@yopmail.com", plan: "business", products: 5, orders: 5 },
];

function loadDbUrl() {
  const propsPath = path.join(__dirname, "..", "troco-dev-db.properties");
  const props = fs.readFileSync(propsPath, "utf8");
  const pwd = (props.match(/troco\.dev\.db\.password=(.+)/) || [])[1]?.trim();
  const url =
    process.env.DB_URL ||
    `postgresql://neondb_owner:${encodeURIComponent(pwd)}@ep-lively-wave-axool097-pooler.c-4.us-east-2.aws.neon.tech/neondb?sslmode=require`;
  return url;
}

async function api(method, urlPath, token, body) {
  const res = await fetch(`${API}${urlPath}`, {
    method,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  let json;
  try {
    json = JSON.parse(text);
  } catch {
    json = { raw: text };
  }
  if (!res.ok) {
    throw new Error(`${method} ${urlPath} -> ${res.status} ${text.slice(0, 400)}`);
  }
  return json;
}

async function ensureStore(saToken, t) {
  const list = await api("GET", "/platform/fournisseurs", saToken);
  let store = (list.data || []).find(
    (f) => f.slug === t.slug || (f.email || "").toLowerCase() === t.email.toLowerCase()
  );
  if (!store) {
    const created = await api("POST", "/platform/fournisseurs", saToken, {
      name: t.name,
      slug: t.slug,
      adminEmail: t.email,
      adminPassword: PASSWORD,
      adminFullName: `Admin ${t.key}`,
      email: t.email,
      planCode: t.plan,
    });
    store = created.data;
    console.log(`Created store ${t.slug} id=${store.id} plan=${store.planCode}`);
  } else {
    console.log(`Store exists ${t.slug} id=${store.id} plan=${store.planCode} status=${store.status}`);
    if ((store.planCode || "").toLowerCase() !== t.plan) {
      await api("PATCH", `/platform/fournisseurs/${store.id}/plan`, saToken, { planCode: t.plan });
      console.log(`  -> plan set to ${t.plan}`);
    }
    if ((store.status || "").toUpperCase() !== "ACTIVE") {
      await api("PATCH", `/platform/fournisseurs/${store.id}/status`, saToken, { status: "ACTIVE" });
      console.log(`  -> status ACTIVE`);
    }
    // Reset admin password via SQL later if needed
  }
  return store;
}

async function seedTenant(client, storeId, t) {
  await client.query("BEGIN");
  try {
    // Category
    let cat = await client.query(
      `SELECT id FROM categories WHERE fournisseur_id = $1 AND slug = 'qa-seed' LIMIT 1`,
      [storeId]
    );
    let categoryId;
    if (cat.rows.length) {
      categoryId = cat.rows[0].id;
    } else {
      const ins = await client.query(
        `INSERT INTO categories (name, description, slug, show_on_hero, fournisseur_id)
         VALUES ($1, $2, 'qa-seed', true, $3) RETURNING id`,
        [`Catalogue ${t.key}`, `Catégorie seed QA ${t.key}`, storeId]
      );
      categoryId = ins.rows[0].id;
    }

    // Soft-delete existing seed products then recreate exact count (keep non-seed products out of way)
    await client.query(
      `UPDATE products SET deleted = TRUE, updated_at = NOW()
       WHERE fournisseur_id = $1 AND deleted = FALSE AND sku LIKE 'QA-%'`,
      [storeId]
    );

    const active = await client.query(
      `SELECT COUNT(*)::int AS c FROM products WHERE fournisseur_id = $1 AND deleted = FALSE`,
      [storeId]
    );
    let need = t.products - active.rows[0].c;
    if (need < 0) {
      // Soft-delete extras (non-QA first, then QA)
      const extras = await client.query(
        `SELECT id FROM products WHERE fournisseur_id = $1 AND deleted = FALSE
         ORDER BY CASE WHEN sku LIKE 'QA-%' THEN 0 ELSE 1 END, id DESC
         LIMIT $2`,
        [storeId, -need]
      );
      for (const row of extras.rows) {
        await client.query(`UPDATE products SET deleted = TRUE, updated_at = NOW() WHERE id = $1`, [row.id]);
      }
      need = 0;
    }

    if (need > 0) {
      const values = [];
      const params = [];
      let p = 1;
      for (let i = 1; i <= need; i++) {
        const n = active.rows[0].c + i;
        values.push(
          `($${p++}, $${p++}, $${p++}, $${p++}, $${p++}, $${p++}, FALSE, FALSE, FALSE, NOW(), NOW(), $${p++})`
        );
        params.push(
          `Produit QA ${t.key} #${n}`,
          `Description seed produit ${n}`,
          99.0 + (n % 10),
          50 + (n % 20),
          `QA-${t.key.toUpperCase()}-${String(n).padStart(4, "0")}`,
          categoryId,
          storeId
        );
      }
      await client.query(
        `INSERT INTO products
          (name, description, price, stock, sku, category_id, deleted, show_weight, customizable, created_at, updated_at, fournisseur_id)
         VALUES ${values.join(",")}`,
        params
      );
    }

    // Customer for orders
    let cust = await client.query(
      `SELECT id FROM customers WHERE fournisseur_id = $1 AND phone = $2 LIMIT 1`,
      [storeId, `06000000${storeId}`]
    );
    let customerId;
    if (cust.rows.length) {
      customerId = cust.rows[0].id;
    } else {
      const ins = await client.query(
        `INSERT INTO customers (full_name, phone, address, city, email, fournisseur_id)
         VALUES ($1, $2, '1 Rue QA', 'Casablanca', $3, $4) RETURNING id`,
        [`Client QA ${t.key}`, `06000000${storeId}`, `client-${t.key}@yopmail.com`, storeId]
      );
      customerId = ins.rows[0].id;
    }

    // Product for order items
    const prod = await client.query(
      `SELECT id, price FROM products WHERE fournisseur_id = $1 AND deleted = FALSE ORDER BY id LIMIT 1`,
      [storeId]
    );
    if (!prod.rows.length) throw new Error(`No product for tenant ${storeId}`);
    const productId = prod.rows[0].id;
    const unitPrice = Number(prod.rows[0].price) || 99;

    // Current-month order count
    const monthCount = await client.query(
      `SELECT COUNT(*)::int AS c FROM orders
       WHERE fournisseur_id = $1
         AND created_at >= date_trunc('month', NOW())
         AND created_at < date_trunc('month', NOW()) + interval '1 month'`,
      [storeId]
    );
    let ordersNeed = t.orders - monthCount.rows[0].c;
    if (ordersNeed < 0) {
      // Delete newest extras in current month (and their items)
      const extras = await client.query(
        `SELECT id FROM orders
         WHERE fournisseur_id = $1
           AND created_at >= date_trunc('month', NOW())
           AND created_at < date_trunc('month', NOW()) + interval '1 month'
         ORDER BY id DESC LIMIT $2`,
        [storeId, -ordersNeed]
      );
      const ids = extras.rows.map((r) => r.id);
      if (ids.length) {
        await client.query(`DELETE FROM order_items WHERE order_id = ANY($1::bigint[])`, [ids]);
        await client.query(`DELETE FROM orders WHERE id = ANY($1::bigint[])`, [ids]);
      }
      ordersNeed = 0;
    }

    if (ordersNeed > 0) {
      const chunk = 200;
      let created = 0;
      const baseSeq = monthCount.rows[0].c;
      while (created < ordersNeed) {
        const n = Math.min(chunk, ordersNeed - created);
        const rowsSql = [];
        const params = [];
        let idx = 1;
        for (let i = 1; i <= n; i++) {
          const seq = baseSeq + created + i;
          rowsSql.push(
            `($${idx++}, $${idx++}, $${idx++}, 'CONFIRMED', 'cash_on_delivery', 'cod', 0,
              NOW() - ($${idx++}::int * interval '1 minute'), NOW(), $${idx++})`
          );
          params.push(
            `${t.key.toUpperCase()}-${String(seq).padStart(6, "0")}`,
            customerId,
            unitPrice,
            i,
            storeId
          );
        }
        const inserted = await client.query(
          `INSERT INTO orders
            (order_number, customer_id, total_amount, status, payment_method, payment_status, shipping_fee, created_at, updated_at, fournisseur_id)
           VALUES ${rowsSql.join(",")}
           RETURNING id`,
          params
        );

        const itemRows = [];
        const itemParams = [];
        let j = 1;
        for (const row of inserted.rows) {
          itemRows.push(`($${j++}, $${j++}, 1, $${j++}, $${j++})`);
          itemParams.push(row.id, productId, unitPrice, unitPrice);
        }
        await client.query(
          `INSERT INTO order_items (order_id, product_id, quantity, unit_price, subtotal)
           VALUES ${itemRows.join(",")}`,
          itemParams
        );
        created += n;
        process.stdout.write(`  orders ${t.key}: ${baseSeq + created}/${t.orders}\r`);
      }
      console.log(`  orders ${t.key}: ${t.orders}/${t.orders}          `);
    }

    // Ensure admin password hash matches Test1234! via API login check — skip bcrypt here.
    // Sync plan_id just in case
    await client.query(
      `UPDATE fournisseurs f SET plan_id = p.id, status = 'ACTIVE'
       FROM plans p WHERE f.id = $1 AND p.code = $2`,
      [storeId, t.plan]
    );

    const finalP = await client.query(
      `SELECT COUNT(*)::int AS c FROM products WHERE fournisseur_id = $1 AND deleted = FALSE`,
      [storeId]
    );
    const finalO = await client.query(
      `SELECT COUNT(*)::int AS c FROM orders
       WHERE fournisseur_id = $1
         AND created_at >= date_trunc('month', NOW())
         AND created_at < date_trunc('month', NOW()) + interval '1 month'`,
      [storeId]
    );
    console.log(
      `OK ${t.email} slug=${t.slug} plan=${t.plan} products=${finalP.rows[0].c} orders_month=${finalO.rows[0].c}`
    );

    await client.query("COMMIT");
  } catch (e) {
    await client.query("ROLLBACK");
    throw e;
  }
}

async function main() {
  const login = await api("POST", "/auth/login", null, {
    email: "superadmin@matjarona.ma",
    password: "SuperAdmin1234",
  });
  const saToken = login.data.access_token;

  const stores = {};
  for (const t of TENANTS) {
    stores[t.key] = await ensureStore(saToken, t);
  }

  const client = new pg.Client({ connectionString: loadDbUrl(), ssl: { rejectUnauthorized: false } });
  await client.connect();
  try {
    for (const t of TENANTS) {
      console.log(`Seeding ${t.key}...`);
      await seedTenant(client, stores[t.key].id, t);
    }
  } finally {
    await client.end();
  }

  console.log("\n=== Comptes QA ===");
  for (const t of TENANTS) {
    console.log(`${t.plan.padEnd(8)} ${t.email} / ${PASSWORD}  slug=${t.slug}  produits=${t.products}  cmd/mois=${t.orders}`);
  }
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});

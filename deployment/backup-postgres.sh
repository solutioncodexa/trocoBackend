#!/usr/bin/env bash
# Sauvegarde PostgreSQL Get STORE.
# DB_URL=jdbc:postgresql://host:5432/troco DB_USER=... DB_PASSWORD=... ./backup-postgres.sh
set -euo pipefail
if [[ -z "${DB_URL:-}" ]]; then
  echo "DB_URL manquant" >&2
  exit 1
fi
raw="${DB_URL#jdbc:}"
hostport="${raw#*://}"
hostport="${hostport%%/*}"
host="${hostport%%:*}"
port="${hostport#*:}"
if [[ "$port" == "$host" ]]; then port=5432; fi
db="${raw#*://}"
db="${db#*/}"
db="${db%%\?*}"
out_dir="$(cd "$(dirname "$0")" && pwd)/backups"
mkdir -p "$out_dir"
out="$out_dir/troco-$(date +%Y%m%d-%H%M%S).dump"
PGPASSWORD="${DB_PASSWORD:-}" pg_dump --format=custom --no-owner --host "$host" --port "$port" --username "${DB_USER:-}" --dbname "$db" --file "$out"
echo "Sauvegarde écrite : $out"

#!/usr/bin/env bash
# =============================================================================
# Bootstrap Troco sur le VPS GoldYara (à lancer une fois en SSH)
# Usage (depuis /opt/goldyara/deployment) :
#   bash scripts/bootstrap-troco-vps.sh
# =============================================================================
set -euo pipefail

DEPLOY_ROOT="${DEPLOY_ROOT:-/opt/goldyara/deployment}"
cd "${DEPLOY_ROOT}"

echo "==> Création des dossiers Troco"
mkdir -p troco/backend troco/frontend/dist docker nginx/conf.d scripts

if [ ! -f .env.troco ]; then
  if [ -f .env.troco.example ]; then
    cp .env.troco.example .env.troco
    echo "Créé .env.troco depuis example — ÉDITER LES SECRETS avant up"
  else
    echo "WARN: .env.troco.example introuvable — créez .env.troco manuellement"
  fi
else
  echo ".env.troco déjà présent"
fi

if [ -f nginx/conf.d/troco.ma.vps.conf ] && [ ! -f nginx/conf.d/troco.ma.conf ]; then
  cp nginx/conf.d/troco.ma.vps.conf nginx/conf.d/troco.ma.conf
  echo "Copié nginx conf Troco → nginx/conf.d/troco.ma.conf"
fi

echo "==> Démarrage stack Troco (postgres + minio + backend + frontend)"
docker compose --env-file .env --env-file .env.troco \
  -f docker-compose.yml \
  -f docker-compose.artifacts.yml \
  -f docker-compose.troco.yml \
  up -d troco-postgres troco-minio troco-minio-init troco-backend troco-frontend

echo "==> Reload nginx edge (si conf Troco présente)"
if docker ps --format '{{.Names}}' | grep -qx goldyara-nginx; then
  docker exec goldyara-nginx nginx -t && docker exec goldyara-nginx nginx -s reload || true
fi

# Certificat :
#   docker compose exec certbot certbot certonly --webroot -w /var/www/certbot \
#     -d getstore.codexa-solution.com -d api.getstore.codexa-solution.com \
#     --email contact@codexa-solution.com --agree-tos --non-interactive \
#     --account 8a26
echo "==> Certificat Let's Encrypt (si domaine pointé)"
echo "  docker compose exec certbot certbot certonly --webroot -w /var/www/certbot \\"
echo "    -d getstore.codexa-solution.com -d api.getstore.codexa-solution.com \\"
echo "    --email contact@codexa-solution.com --agree-tos --non-interactive \\"
echo "    --account 8a26"
echo "DONE."

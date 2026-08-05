# Troco / Matjarona — déploiement

Deux modes :

1. **Stack autonome** (`docker-compose.yml` + artifacts) — `/opt/troco/deployment`
2. **Sidecar VPS GoldYara** (recommandé, comme Codexa / Caldeals) — `/opt/goldyara/deployment`

## Sidecar sur le même VPS (GoldYara)

### Fichiers à déposer une fois

Sous `/opt/goldyara/deployment/` :

| Fichier | Rôle |
|---|---|
| `docker-compose.troco.yml` | Services `troco-*` |
| `.env.troco` | Secrets prod (DB, JWT, MinIO, SMTP) |
| `docker/nginx-internal-troco.conf` | Nginx SPA → `troco-backend` |
| `nginx/conf.d/troco.ma.conf` | Vhost edge HTTPS |
| `troco/backend/troco-backend.jar` | Artifact CI |
| `troco/frontend/dist/` | Artifact CI |

### Bootstrap

```bash
cd /opt/goldyara/deployment
# copier les fichiers depuis le repo trocoBackend/deployment/
cp .env.troco.example .env.troco
nano .env.troco   # DB + JWT + MinIO + SMTP LWS (mêmes valeurs mail.goldyara.com que GoldYara)

bash scripts/bootstrap-troco-vps.sh
# ou :
docker compose --env-file .env --env-file .env.troco \
  -f docker-compose.yml -f docker-compose.artifacts.yml -f docker-compose.troco.yml \
  up -d troco-postgres troco-minio troco-minio-init troco-backend troco-frontend
```

Certificat :

```bash
docker compose exec certbot certbot certonly --webroot -w /var/www/certbot \
  -d troco.ma -d www.troco.ma --email contact@goldyara.com --agree-tos --non-interactive
docker exec goldyara-nginx nginx -t && docker exec goldyara-nginx nginx -s reload
```

### CI / CD (GitHub Actions)

Branches déployées : **`feature/shopify`**, `shopify`, `main`, `master`.

- Backend : `trocoBackend/.github/workflows/ci-backend.yml` → `troco-backend`
- Frontend : `trocoFronend/.github/workflows/ci-frontend.yml` → `troco-frontend`

Secrets / vars GitHub (mêmes que GoldYara) :

- `VPS_HOST` / `VPS_USER` / `VPS_SSH_PRIVATE_KEY`
- Optionnel : `VPS_DEPLOY_PATH=/opt/goldyara/deployment`, `VITE_API_BASE_URL=/api`

### SMTP partagé

Dans `.env.troco` (profil `prod`) :

```
MAIL_HOST=mail.goldyara.com
MAIL_PORT=465
MAIL_USERNAME=noreply@goldyara.com
MAIL_PASSWORD=<même mot de passe GoldYara>
MAIL_SSL=true
MAIL_STARTTLS=false
```

### Base prod

Postgres dédié `troco-postgres` (volume `troco_pg_data`), DB `troco`.  
Profil Spring : `SPRING_PROFILES_ACTIVE=prod` (`application-prod.properties`).

## Stack autonome (legacy)

```bash
cd /opt/troco/deployment   # ou trocoBackend/deployment en local
cp .env.example .env
docker compose -f docker-compose.yml -f docker-compose.artifacts.yml up -d
```

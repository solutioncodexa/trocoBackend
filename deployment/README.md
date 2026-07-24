# Troco — déploiement Docker

Stack : **Postgres** + **MinIO** + **Backend** + **Frontend (Nginx)** + **reverse-proxy Nginx**.

## Contenu

| Fichier / dossier | Rôle |
|---|---|
| `docker-compose.yml` | Services de base |
| `docker-compose.build.yml` | Build local depuis les Dockerfiles |
| `docker-compose.artifacts.yml` | VPS / CI (JAR + `dist`) |
| `nginx/` | Reverse proxy + conf SPA frontend |
| `troco/backend/` | Emplacement du JAR CI |
| `troco/frontend/dist/` | Emplacement du build front CI |
| `certs/` | Certificats TLS (optionnel) |

## Démarrage local

```bash
cd deployment
cp .env.example .env
# Éditer DB_PASSWORD, MINIO_SECRET_KEY, APP_JWT_SECRET

docker compose -f docker-compose.yml -f docker-compose.build.yml up -d --build
```

- Site : http://localhost  
- API : http://localhost/api  
- MinIO console : http://localhost:9001  
- Fichiers : http://localhost/files/`<bucket>`/`<object>`

## VPS (aligné GitHub Actions)

Chemin attendu : `/opt/troco/deployment`

```bash
cd /opt/troco/deployment
cp .env.example .env   # une seule fois
# Déposer :
#   troco/backend/troco-backend.jar
#   troco/frontend/dist/*

docker compose -f docker-compose.yml -f docker-compose.artifacts.yml up -d
```

Redémarrage partiel (CI) :

```bash
docker compose -f docker-compose.yml -f docker-compose.artifacts.yml up -d --force-recreate --no-deps backend
docker compose -f docker-compose.yml -f docker-compose.artifacts.yml up -d --force-recreate --no-deps frontend
```

## Déjà présent ailleurs dans le projet

- Backend : client MinIO Java (`MinioConfig`, `MinioStorageService`)
- Frontend : `Dockerfile` multi-stage + `nginx.conf` (SPA)

# Get STORE / Troco — Architecture technique

## 1. Organisation du code

Le dossier workspace `troco/` n’est **pas** un monorepo Git unique :

| Repo | Chemin | Remote |
|---|---|---|
| Backend | `trocoBackend/` | `solutioncodexa/trocoBackend` |
| Frontend | `trocoFronend/` | `solutioncodexa/trocoFronend` |

Branche feature courante : `feature/shopify`.

---

## 2. Stack

### Backend
- **Java 21**, **Spring Boot 4.0.2**
- Spring Security + JWT (jjwt 0.12.6)
- Spring Data JPA / Hibernate
- Flyway (migrations `V18` → `V27` multi-tenant & features)
- PostgreSQL (dev : Neon ; prod Docker : Postgres 16)
- MinIO client 8.5.17 (ou stockage local)
- MapStruct, springdoc OpenAPI
- Bucket4j (rate limit)
- ImageIO + webp-imageio (optimisation upload)

### Frontend
- **React 18**, **TypeScript**, **Vite 5**
- Tailwind CSS + composants Radix/shadcn
- TanStack Query, React Router
- Vitest + Testing Library
- Playwright (E2E)
- Port dev : **4200** (proxy `/api` → `127.0.0.1:8080`)

---

## 3. Architecture applicative

```
[ Navigateur :4200 ]
        │  /api/* (proxy Vite)
        ▼
[ Spring Boot :8080/api ]
        │
        ├── Neon / Postgres
        ├── MinIO :9000 (optionnel)
        └── SMTP (mail)
```

### Multi-tenant
- Entités marquées `@TenantScoped` → filtre Hibernate `fournisseur_id`
- `TenantResolutionFilter` : header slug/id → query `tenant` → host/domaine
- `TenantContext` (thread-local) ; `SUPER_ADMIN` bypass

### Auth
- Access JWT (Bearer) + refresh token en base
- Rôles : `SUPER_ADMIN` | `ADMIN` | `STAFF`
- Permissions granulaires pour STAFF (`AppPermissions` + `@RequirePermission`)

### Stockage
- `app.storage.type=local|minio`
- Façade `FileStorageService` → `LocalStorageService` / `MinioStorageService`
- `ImageOptimizeService` (resize + WebP si dispo)

---

## 4. Packages clés

**Backend** (`ma.codexa.troco`)  
`config` · `security` · `tenant` · `controller` · `service` (+ `storage`) · `entity` · `repository` · `dto` · `storefront` · `db/migration`

**Frontend** (`src/`)  
`pages` · `components` · `contexts` · `services/api` · `hooks` · `config` · `e2e`

---

## 5. Profils & config

| Profil | Usage |
|---|---|
| `dev` | Local + Neon, HSTS off, CORS localhost |
| `test` | H2 mémoire, Flyway off, storage local |
| `prod` | Env vars obligatoires, MinIO, HSTS |

Secrets : `troco-dev-db.properties` / `.env` (gitignore).  
Deployment Docker : `trocoBackend/deployment/` (Postgres + MinIO + backend + frontend + Nginx).

---

## 6. CI / CD

**Backend** (`.github/workflows/ci-backend.yml`)  
`mvn test` → `package` → artifact JAR → deploy VPS (docker recreate backend)

**Frontend** (`.github/workflows/ci-frontend.yml`)  
`npm ci` → `build` → **Playwright E2E** → artifact dist → deploy VPS

---

## 7. Tests

| Niveau | Où |
|---|---|
| Unit BE | `src/test/.../unit/` |
| Intégration BE | `src/test/.../integration/` + `IntegrationTestBase` |
| Unit FE | `src/**/*.test.ts(x)` (Vitest) |
| E2E FE | `e2e/*.spec.ts` (Playwright, mocks API) |

---

## 8. Démarrage local (rappel)

```bash
# MinIO
docker run -d --name troco-minio-dev -p 9000:9000 -p 9001:9001 \
  -e MINIO_ROOT_USER=trocominio -e MINIO_ROOT_PASSWORD=minioDevSecret123 \
  minio/minio server /data --console-address ":9001"

# Backend
cd trocoBackend
# STORAGE_TYPE=minio MINIO_ENDPOINT=http://127.0.0.1:9000 ...
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Frontend
cd trocoFronend
npm run dev -- --host 127.0.0.1 --port 4200
```

Voir aussi `trocoBackend/deployment/README.md`.

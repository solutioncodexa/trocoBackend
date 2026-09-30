# Get STORE / Troco — Sécurité

## 1. Authentification

| Mécanisme | Détail |
|---|---|
| Access token | JWT HS512, Bearer `Authorization` |
| Refresh token | UUID stocké en base, rotation login/refresh |
| Mots de passe | BCrypt (force 12) |
| Rôles | `SUPER_ADMIN`, `ADMIN`, `STAFF` |
| Front | Token dans `localStorage` (`troco_admin_token`) |

Endpoints publics auth : `/auth/login`, `/register`, `/refresh`, `/logout`, `/me` (me authentifié).

---

## 2. Autorisation

- **ADMIN / SUPER_ADMIN** : bypass des permissions granulaires
- **STAFF** : codes `AppPermissions`  
  `PRODUCTS_*`, `ORDERS_*`, `STOCK_*`, `CUSTOM_ORDERS_*`, `CATALOG_MANAGE`, `CONTENT_MANAGE`, `STATS_VIEW`, `MEMBERS_MANAGE`, `AUDIT_VIEW`, `PAGES_EDIT`, `PAGES_PUBLISH`, `WEBHOOKS_MANAGE`
- Annotation `@RequirePermission` + aspect AOP
- Implications (ex. `PAGES_PUBLISH` ⇒ `PAGES_EDIT`)

Front : `ProtectedAdminRoute` + `PERMISSIONS` miroir.

---

## 3. Isolation multi-tenant

1. Résolution tenant (header / query / host / domaine)
2. `TenantContext` thread-local
3. Filtre Hibernate `fournisseurFilter` sur entités `@TenantScoped`
4. JWT injecte `fournisseurId` si contexte vide
5. `SUPER_ADMIN` : bypass filtre (ops plateforme)

**Risque à surveiller** : un header `X-Fournisseur-Slug` peut primer si le JWT ne force pas le tenant — tester l’isolation (cf. `TenantIsolationIntegrationTest`).

---

## 4. Protection HTTP

| Contrôle | Comportement |
|---|---|
| CORS | Origines configurables (`app.cors.*`), credentials |
| Rate limit | Bucket4j par IP (avant JWT) ; **login exclu** |
| Headers | `X-Frame-Options: DENY`, nosniff, Referrer-Policy, Permissions-Policy |
| HSTS | Activé en prod uniquement |
| CSRF | Désactivé (API JWT stateless) |

---

## 5. Surfaces publiques

Exemples `permitAll` :
- Catalogue GET, `*/public/**`
- `POST /orders`, panier, wishlist
- Inscription plateforme, plans, store public
- Callbacks CMI
- Upload logo
- Actuator : `health`, `info`, **`prometheus`** (à restreindre au reverse-proxy)

Tout le reste : `authenticated()` (+ rôles selon endpoint).

---

## 6. Uploads

- Taille : fichier 10 Mo / requête 50 Mo (logo 5 Mo)
- Types : jpeg, png, gif, webp, pdf (content-type + extension après optim)
- Stockage local ou MinIO (`public-read` configurable)
- Optimisation image serveur (réduction surface attaque payload)

---

## 7. Secrets & config

| Bonne pratique | État |
|---|---|
| Secrets hors Git | `.env`, `troco-dev-db.properties` gitignorés |
| Prod via env | `DB_*`, `APP_JWT_SECRET`, MinIO, mail |
| JWT secret fort | Obligatoire en prod |

**Écarts connus (dev)**  
- Secret JWT / mots de passe SuperAdmin & Admin par défaut dans `application-dev.properties`  
- Host Neon (username) versionné ; password externalisé  
- MinIO `public-read` souvent `true` en démo

---

## 8. Webhooks

- Signature optionnelle HMAC-SHA256 (`X-Matjarona-Signature`)
- Secret par endpoint webhook
- Journal des livraisons (admin)

---

## 9. Conformité loi 09-08 (CNDP) — Maroc

Get STORE traite des **données personnelles** pour le compte de chaque boutique (commandes, leads, avis, paniers abandonnés, contacts WhatsApp, pixels marketing). La loi **09-08** et le contrôle CNDP s’appliquent ; depuis 2025 les contrôles actifs exposent à des amendes jusqu’à **300 000 MAD**.

| Obligation | État produit | Risque |
|---|---|---|
| Cadre responsable / sous-traitant (DPA SaaS ↔ marchand) | Hors code (contrat) | Élevé |
| Information des personnes (privacy + mentions) | URL privacy + pages CMS | Moyen |
| Consentement cookies / traceurs (pixels) | **Livré** — bannière + gate pixels | Faible |
| Droits d’accès / effacement | **Livré** — admin Conformité export/erase + audit | Moyen (pas encore self-service client) |
| Finalités & rétention | **Partiel** — `dataRetentionDays` ; purge auto à brancher | Moyen |
| Sécurité des traitements (auth, isolation tenant, audit) | Partiel — voir §3–8 | Moyen |
| PCI-DSS / sécurité paiement carte | Flags CMI/BNPL + **payment_audit_logs** ; pas de carte hébergée | Bloquant dès CMI réel |

**Avant lancement commercial** (reste juridique / ops) :
1. DPA + déclaration CNDP formelle  
2. Job de purge selon `dataRetentionDays`  
3. Templates privacy FR/AR par défaut à l’inscription boutique  
4. Dès CMI boutique : PSP hébergé (réduire scope PCI) + conserver les logs d’audit

---

## 10. Recommandations prioritaires

1. **Pack CNDP / loi 09-08** (consentement, privacy, droits, rétention, DPA)  
2. **Restreindre** `/actuator/prometheus` (et idéalement actuator) hors réseau public  
3. **Rate-limit** `/auth/login` (anti brute-force)  
4. **Forcer le tenant JWT** pour ADMIN/STAFF (ignorer slug étranger)  
5. Secrets prod uniquement via vault/env ; rotation JWT  
6. Envisager httpOnly cookies pour les tokens (réduire XSS localStorage)  
7. CSP / sanitize contenu rich-text pages builder  
8. Audit régulier des endpoints `permitAll` + accès PII  
9. Tests d’isolation tenant en CI bloquants
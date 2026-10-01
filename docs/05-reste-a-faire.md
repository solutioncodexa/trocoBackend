# Get STORE / Troco — Reste à faire

Priorisation indicative (P0 = bloquant / risque, P1 = fort impact, P2 = nice-to-have).

---

## P0 — Sécurité & prod

| Item | Notes |
|---|---|
| Restreindre Actuator / Prometheus | Ne pas exposer publiquement |
| Rate-limit login | Anti brute-force |
| Tenant JWT strict | Empêcher hop cross-tenant via header slug |
| Secrets prod | Pas de defaults JWT / admin en prod |
| Auth `gh` + ouvrir les PR | Branches `feature/shopify` déjà poussées |
| Déclaration CNDP formelle | Pack produit livré (consentement, export/erase, rétention) — formalités juridiques marchand / DPA à finaliser hors code |

Liens PR (à finaliser après `gh auth login`) :
- https://github.com/solutioncodexa/trocoFronend/pull/new/feature/shopify
- https://github.com/solutioncodexa/trocoBackend/pull/new/feature/shopify

---

## P1 — Approfondir les MVPs 2026

| Item | Livré (MVP) | Suite |
|---|---|---|
| Recherche + facettes | `/products/facets` + filtres taille/prix/catégorie | Facettes dynamiques attributs Woo, tri popularité serveur |
| Reco produits | Co-achat + fallback catégorie (`/recommendations`) | Scoring comportemental / similarité |
| Multi-langue | Shell UI FR/AR/EN + RTL | Traduction catalogue / pages CMS |
| Multi-devise | Devise boutique + taux JSON + affichage | Taux live / FX provider |
| Transporteurs | Amana/CTM/DHL barèmes + tracking URL | APIs transporteurs temps réel |
| BNPL / CMI boutique | Flags checkout + audit paiement | PSP hébergé réel + PCI scope |
| Fidélité | Points earn/redeem | Tiers, expirations, campagnes |
| API headless | `/headless/v1` + clés API | OpenAPI public, webhooks inbound, rate-limit par clé |
| CNDP | Bannière cookies, privacy URL, export/erase, rétention | DPA contrat, purge auto job, portabilité JSON-LD |

---

## P1 — Ops déjà listés

| Item | Notes |
|---|---|
| WhatsApp Cloud API (option) | Actuel = click-to-chat `wa.me` uniquement |
| Relance panier email fiable | Dépend SMTP prod + templates |
| Webhooks retry / DLQ | Journal existe ; stratégie retry à formaliser |
| Domaine custom SSL UX | Assistant DNS (CNAME + copie) + vérif ; certificat auto Cloudflare / plateforme |
| Import catalogue massif | Endpoint import présent ; UX admin à renforcer |

---

## P1 — Qualité / CI

| Item | Notes |
|---|---|
| Vitest dans CI frontend | Aujourd’hui E2E seulement |
| Tests intégration gaps 2026 | shipping, loyalty, privacy, headless |
| Health check utile | `/actuator/health` 503 (souvent mail) — health groups |
| Docs dans un repo Git | Dossier `docs/` au niveau workspace (hors git) |

---

## P2 — UX / polish

| Item | Notes |
|---|---|
| Builder drag-and-drop avancé | Blocs OK ; DnD / responsive preview |
| Mega-menu mobile | Desktop testé ; mobile à peaufiner |
| Accessibilité admin | Focus traps, labels |
| Onboarding marchand | Checklist + case conformité CNDP |

---

## Suite parcours autonome (à traiter)

| Item | Notes |
|---|---|
| Builder pour pages boutique | Fiche produit, liste, panier, checkout : pas de builder de blocs (apparence globale seulement) |
| Activation auto + paiement plan (CMI) | Volontairement non appliqué : fin d’essai → activation manuelle Super Admin |
| Emails clients : templates HTML / personnalisation marchand | Textes simples actuellement |
| Transporteurs temps réel | Barèmes + URL de suivi livrés ; APIs Amana/CTM/DHL live à brancher |
| PSP boutique réel | Flags COD / CMI / BNPL + audit ; redirect CMI / PCI hors scope MVP |

## Déjà livré récemment

- **Styles complets dans Paramètres → Apparence** (même grille que l’assistant : thème + couleurs + polices + arrondis + agencement). Changer de thème seul, plus bas, conserve les couleurs déjà enregistrées.
- **SEO catégories** (admin + meta OG / canonical sur `/boutique?category=`) et **SEO blog** (liste + article : title, description, OG, canonical)
- **Assistant domaine personnalisé** (saisie, CNAME à copier, HTTPS, vérifier DNS)
- Liens pages légales dans le pied de page
- **Aperçu du builder dans un iframe** (bureau 1100 / tablette 768 / mobile 390 : les media queries s’appliquent réellement), zoom auto-ajusté
- **Essai gratuit 30 j**, vérification email, onboarding (secteur, produits démo, pages légales), 6 nouveaux blocs + 3 modèles, emails clients, SEO produit  

- Page builder, SEO, sections globales, A/B home  
- Webhooks, leads, blog, avis, panier abandonné  
- WhatsApp Business + guide Meta + sync sociaux  
- **Facettes catalogue, i18n shell, multi-devise affichage**  
- **Transporteurs + frais + tracking template**  
- **COD / CMI / BNPL flags + payment audit**  
- **Fidélité points, API headless, pack CNDP UI**  
- E2E Playwright + intégration tests + CI  
- MinIO + optim images  

---

## Prochaine itération suggérée

1. Sécurité technique P0 (login rate-limit + tenant JWT + actuator)  
2. Tests auto sur shipping / privacy / headless  
3. CMI boutique réel (PSP redirect) ou formaliser « COD-only » en prod  
4. Finaliser PR `feature/shopify` + versionner `docs/`  

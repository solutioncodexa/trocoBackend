# Get STORE / Troco — Reste à faire

Priorisation indicative (P0 = bloquant / risque, P1 = fort impact, P2 = nice-to-have).

## Lot en cours — vérifié dans le code

| Item | État |
|---|---|
| Tests d'intégration 403 « boutique ouvre bientôt » | Corrigé : `EmailVerification` et `StoreSettings` lancent la boutique avant l'appel public |
| Doublons sous-catégories (saisie multiple) | Ignorés comme l'ajout rapide |
| Confirmation AdminCategories | Même `useConfirm` que les produits |
| Emails HTML | Commande (client + admin), essai, panier abandonné |
| WebSocket | Abonnement STOMP limité à `/topic/store.{id}` de la boutique connectée |
| Variantes panier « 10 » / « 20 » | Le libellé part dans la commande et le résumé checkout (pas seulement la taille) |
| Packs marocains | Noms/descriptions arabes si l'admin est en arabe ; prix MAD inchangés |
| Suppression produits d'exemple | L'erreur serveur est remontée dans le toast (cause exacte à lire si ça échoue encore) |

## Tests à ajouter pour ce lot

- Intégration : commande avec deux variantes du même produit (libellés 10 et 20) → deux lignes
- Intégration : `GET /platform/store` avant lancement → 403, après lancement → 200
- WebSocket : abonnement à `/topic/store.{autre}` refusé
- Frontend : saisie multiple de sous-catégories ignore les doublons
- Playwright : création boutique, commande COD, publication de page
- Les 210 cas manuels documentés restent à exécuter (non automatisés ici)

## Priorité marché marocain

Livré dans le code (migration V49, page admin Marché, checkout) :

- Statuts commande : appel client, injoignable, retour (le retour remet le stock). Le COD est mis en avant au checkout.
- Frais par ville pour Amana, Chronodiali et Glovo
- WhatsApp de confirmation en français, darija latine et darija arabe
- PayZone et virement comme drapeaux (commande en attente, pas un PSP réel). CMI reste un flag côté passerelle.
- RTL du bandeau boutique quand la langue est l’arabe
- Catalogue Meta : `GET /api/catalog/meta.csv`
- Campagnes Ramadan, Aïd et rentrée
- Parrainage, export CSV des commandes, demande `/retours` et remboursement admin
- Playwright : `e2e/market-morocco.spec.ts`
- Sauvegardes : `deployment/backup-postgres.ps1` et `backup-postgres.sh`

Toujours hors de ce lot : téléphone normalisé partout, push vendeur, dashboard mobile dédié, supervision prod, APIs transporteurs.

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
| Domaine custom SSL UX | Vérif DNS + certificat automatisé |
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
| Liens pages légales dans le pied de page | Mentions, confidentialité, CGV (`/page/…`) + retours CMS si pas de page `livraison-retours` |
| Builder pour pages boutique | Fiche produit, liste, panier, checkout : réglages d’apparence seulement |
| SEO catégories / blog | SEO personnalisé disponible pour produits et pages uniquement |
| Activation auto + paiement plan (CMI) | Volontairement non appliqué : fin d’essai → activation manuelle Super Admin |
| Domaine custom guidé | Vérif DNS existante ; assistant pas à pas + SSL auto à faire |
| Emails clients : templates HTML / personnalisation marchand | Textes simples actuellement |

## Déjà livré récemment

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

# Get STORE / Troco — Fonctionnalités

Plateforme multi-tenant type Shopify : chaque boutique (`fournisseur`) a sa vitrine, son admin et sa config.

---

## 1. Plateforme Get STORE

| Fonctionnalité | Description |
|---|---|
| Landing Get STORE | Présentation plateforme (`/`, `/matjarona`) |
| Création boutique | Inscription publique → **essai gratuit 30 jours** (statut **TRIAL**, validation automatique, vitrine en ligne immédiatement) (`/creer-boutique`) |
| Cycle d’essai | Fin d’essai → **PENDING** (vitrine fermée, admin accessible) ; le Super Admin passe la boutique en **ACTIVE** (plan) ou prolonge l’essai. Rappel email à J-3 et à l’expiration. Durée : `app.trial.days` |
| Vérification email | Lien de confirmation (48 h) envoyé à l’inscription ; **non bloquant** (bannière + renvoi) |
| Super Admin | Activation boutiques, liste fournisseurs, **configuration des packs** (`/super-admin/packs`) |
| Perf API | DTOs à la demande : bootstrap/checkout ; pages/blog ; catégories nav/cards/hero ; wishlist/cart recover by-ids ; avis paginés ; paniers abandonnés sans JSON ; leads preview ; stock variantes paginé ; webhooks sans secret ; POST /orders → OrderCreatedDTO ; custom-orders status list slim ; members permissions STAFF-only |
| Plans | Packs éditables par Super Admin (prix, limites, features, actif) — seed initial Basic ~79 / Pro ~199 / Business ~399 DH |
| Thèmes | Catalogue classic, minimal, bold, elegant + démos live |

**Comptes types (dev)**  
- Super Admin : `superadmin@matjarona.ma` → Packs : `/super-admin/packs`  
- Admin boutique démo : `admin@troco.ma`

---

## 2. Admin boutique

### Catalogue & commandes
- Produits (CRUD, images, variantes, stock)
- Catégories, catégories hero, produits mis en avant
- Commandes (liste, statuts, suivi colis / tracking)
- Demandes sur-mesure / personnalisations
- Stock (vue, ajustements, mouvements)
- Codes promo
- Transporteurs (Amana, CTM, DHL…) — frais, seuils, URL de suivi

### Contenu & marketing
- Top-bar messages
- Modales promo
- Réseaux sociaux (sync depuis Paramètres)
- Blog (CRUD)
- Leads / formulaires + export CSV
- Avis produits (modération)

### Page builder & vitrine
- Pages (accueil A/B, clone, import/export JSON, analytics)
- Constructeur live type GenCodex (palette | canvas boutique header/footer | propriétés | aperçu live | zoom | structure | undo/redo)
- Modèles prêts (accueil, à propos, lookbook, promo flash, capture leads) + démarrages rapides
- Style sections : alignement, couleurs, espacement, largeur, hauteur bannière, colonnes grille
- Thèmes rapides (pastilles) appliqués aux sections
- App bar personnalisable (couleurs, bandeau, icônes, logo) — section globale `app_bar`
- Mock data atelier si catalogue vide (produits, catégories, avis…)
- **19 blocs** dont Avantages, Newsletter, Galerie, Image + texte, Derniers articles (blog), Logos partenaires ; modèles Accueil complet / Notre histoire / Landing offre
- Actions section : dupliquer, monter/descendre, masquer mobile/bureau ; panneaux masquables ; plein écran
- Versions + restauration
- Preview token
- Promotion variante A/B gagnante
- Sections globales : mega-menu, liens footer, sticky CTA, app bar
- AI copy (templates FR, sans LLM externe)

### Conversion & intégrations
- Pixels Meta / TikTok / Google Analytics / Ads (**gated** par consentement cookies)
- Panier abandonné (délai, liste admin)
- WhatsApp Business (numéro + modèle message commande)
- Webhooks sortants (`order.created`, `lead.created`) + journal de livraisons
- Fidélité / points clients (paramétrable)
- Paiements boutique : COD, CMI (flag), BNPL (flag) + journal d’audit paiement

### Ops & conformité
- **Onboarding** : secteur (6 packs) → thème conseillé, produits d’exemple supprimables (SKU `DEMO-`), pages légales générées (mentions, CGV, retours, confidentialité — modèles à faire valider)
- **Emails clients** : confirmation de commande, expédition (suivi), statut confirmée / livrée / annulée
- **SEO produit** : titre et description meta personnalisables ; canonical = domaine réel de la boutique
- Paramètres boutique (marque, thème, contact, domaine, pixels, **langue/devise**, paiements, fidélité, CNDP)
- Membres STAFF + permissions (scopés par boutique ; Super Admin voit tout)
- Journal d’audit
- Conformité CNDP : export / anonymisation sujet (email/téléphone)
- Clés API headless (`/admin/api-keys`)
- Dashboard / stats / revenus
- Notifications admin

---

## 3. Vitrine client

| Parcours | Routes |
|---|---|
| Accueil / boutique | `/`, `/accueil`, `/boutique` (facettes prix / catégorie / taille) |
| Fiche produit | `/produit/:id` (+ avis, upsell / recommandations, WhatsApp) |
| Panier / checkout | `/panier`, `/checkout` (transporteur, COD / CMI / BNPL, fidélité) |
| Favoris | `/favoris` |
| Pages CMS | `/page/:slug`, `/preview/:token` |
| Blog | `/blog`, `/blog/:slug` |
| Infos | `/contact`, `/faq`, `/livraison-retours` |
| Sur-mesure / devis | `/sur-mesure`, `/devis` |
| Codes promo | `/codes-promo` |
| SEO | `/sitemap.xml`, `/robots.txt` |

Extras : bouton WhatsApp flottant, recover panier (`?recover=`), sticky CTA, mega-menu, footer custom, **bannière cookies**, **sélecteur FR/AR/EN**, affichage multi-devise.

---

## 4. Multi-tenant

Résolution boutique par :
1. Header `X-Fournisseur-Slug` / `X-Fournisseur-Id`
2. Query `?tenant=<slug>`
3. Sous-domaine `{slug}.getstore.com`
4. Domaine personnalisé (vérification DNS)

---

## 5. Stockage médias

- Local (`uploads/`) ou **MinIO** (S3)
- Upload image + optimisation serveur (redimensionnement / WebP)
- Logo boutique (endpoint dédié)

---

## 6. API publique / headless

Auth : header `X-Api-Key` (clés créées en admin).

| Endpoint | Usage |
|---|---|
| `GET /headless/v1/products` | Catalogue paginé |
| `GET /headless/v1/products/{id}` | Détail |
| `POST /headless/v1/orders` | Création commande |
| `GET /headless/v1/orders/{orderNumber}` | Lecture commande |

---

## 7. Conformité CNDP (loi 09-08)

- Consentement cookies avant pixels marketing
- URL politique de confidentialité + rétention configurable
- Export / effacement des données sujet (admin Conformité)
- Audit des exports / anonymisations

---

## 8. Améliorations prévues (hors MVP actuel)

| Domaine | Suite |
|---|---|
| Paiement carte | Connexion CMI boutique réelle (aujourd’hui flag + audit ; billing SaaS CMI séparé) |
| BNPL | Branchement provider (aujourd’hui mode manuel / flag) |
| Transporteurs | API Amana/CTM/DHL live (aujourd’hui barèmes + templates de suivi) |
| Reco | Enrichir le moteur co-achat (embeddings / LLM optionnel) |
| i18n | Traduction complète catalogue / CMS (UI shell FR/AR/EN livrée) |

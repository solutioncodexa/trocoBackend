# Get STORE / Troco — Scénarios à tester

Checklist manuelle + référence aux tests automatisés existants.

**Prérequis locaux**  
- Front : http://127.0.0.1:4200  
- API : http://127.0.0.1:8080/api  
- MinIO (optionnel) : http://127.0.0.1:9001  
- Boutique : `?tenant=<slug>` (ex. `troco`, `maison-atlas`)

---

## A. Plateforme

| # | Scénario | Attendu |
|---|---|---|
| A1 | Landing sans tenant | Page Get STORE, plans visibles |
| A2 | Créer une boutique | Compte TRIAL (30 j), vitrine accessible, bannière essai, email de vérification |
| A3 | Super Admin login | Accès dashboard + liste fournisseurs + Packs |
| A4 | Fin d’essai (TRIAL échu) → PENDING ; Super Admin « Activer le plan » → ACTIVE | Vitrine fermée puis accessible |
| A5 | Changer plan boutique | Limites plan mises à jour |
| A6 | Configurer un pack (prix / limites / features) | Landing et entitlements reflètent la config |

**Auto** : `e2e/matjarona-landing.spec.ts`, `e2e/create-store.spec.ts`, `e2e/super-admin.spec.ts`, `PlatformIntegrationTest`

---

## B. Auth & admin boutique

| # | Scénario | Attendu |
|---|---|---|
| B1 | Login admin valide | Redirect `/admin/dashboard` |
| B2 | Login invalide | Message d’erreur, pas de token |
| B3 | Accès module sans permission (STAFF) | Refus / redirect |
| B4 | Membres : créer STAFF + permissions | Connexion STAFF limitée |
| B5 | Audit : action visible | Entrée journal |

**Auto** : `e2e/admin-login.spec.ts`, `e2e/admin-modules.spec.ts`, `AuthIntegrationTest`

---

## C. Catalogue & commandes

| # | Scénario | Attendu |
|---|---|---|
| C1 | Créer produit + image | Visible en boutique |
| C2 | Parcours boutique → PDP → panier → checkout | Commande COD confirmée |
| C3 | Code promo au checkout | Remise appliquée |
| C4 | Commande admin : changer statut | Statut mis à jour |
| C5 | Stock : ajustement | Quantité / mouvement cohérents |
| C6 | Sur-mesure / devis | Demande créée côté admin |

**Auto** : `e2e/storefront-browse.spec.ts`, `e2e/storefront-checkout.spec.ts`, `CatalogOrderIntegrationTest`, `OrderManagementIntegrationTest`

---

## D. Page builder & sections

| # | Scénario | Attendu |
|---|---|---|
| D1 | Créer page + publier | Visible `/page/:slug` |
| D2 | Accueil A/B + promouvoir gagnant | Une seule home live |
| D3 | Preview token | Page visible sans publish |
| D4 | Mega-menu (desktop) | Sous-liens au hover |
| D5 | Footer links | Colonnes custom cliquables |
| D6 | Sticky CTA | Affichage + dismiss session |
| D7 | Sitemap / robots | XML / texte corrects |

**Auto** : `e2e/storefront-mega-menu.spec.ts`, `e2e/storefront-footer.spec.ts`, `e2e/storefront-conversion.spec.ts`, `e2e/storefront-content.spec.ts`, `StorePagesLeadsWebhooksIntegrationTest`

---

## E. Conversion

| # | Scénario | Attendu |
|---|---|---|
| E1 | WhatsApp : numéro en Paramètres | Bouton flottant + CTA produit/panier `wa.me` |
| E2 | Sync sociaux FB/IG/TikTok | Réseaux activés avec URL |
| E3 | Avis : soumettre → approuver → public | Note / texte sur PDP |
| E4 | Panier abandonné : email checkout → recover | Panier restauré |
| E5 | Upsell « souvent achetés » | Produits liés checkout/PDP |
| E6 | Webhook `order.created` | Delivery log OK / retry visible |
| E7 | Lead formulaire → admin + CSV | Ligne + export |
| E8 | Blog liste + article | Contenu publié |

**Auto** : `e2e/storefront-whatsapp.spec.ts`, `e2e/storefront-abandoned.spec.ts`, `e2e/storefront-conversion.spec.ts`, `AbandonedCartIntegrationTest`, `StoreSettingsIntegrationTest`

---

## F. Multi-tenant & isolation

| # | Scénario | Attendu |
|---|---|---|
| F1 | Deux boutiques, produits distincts | Pas de fuite cross-tenant |
| F2 | `?tenant=` vs header slug | Même boutique résolue |
| F3 | Boutique PENDING | Vitrine refusée (403 / message) |

**Auto** : `TenantIsolationIntegrationTest`, `e2e/storefront-tenant.spec.ts`

---

## G. Médias & perf

| # | Scénario | Attendu |
|---|---|---|
| G1 | Upload image produit (local ou MinIO) | URL accessible |
| G2 | Upload WebP / grande image | Optimisation sans erreur |
| G3 | Logo boutique | Affiché header/footer |

---

## H. Régression CI

```bash
# Backend
cd trocoBackend && ./mvnw test

# Frontend
cd trocoFronend && npm test && npm run test:e2e
```

CI : tests Maven avant package ; Playwright après build front (bloque le deploy).

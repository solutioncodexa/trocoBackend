# 09 — Application installable (administration)

Fichiers : `public/admin.webmanifest` (portée `/admin`, mode `standalone`, icônes 192/512/maskable),
`public/admin-sw.js` (service worker **sans cache** : toutes les requêtes GET passent par le réseau),
`src/hooks/useAdminPwa.ts` (déclare le manifeste et enregistre le service worker **uniquement dans l'admin**).

Prérequis : site servi en **HTTPS**.

---

## PWA-01 — Manifeste déclaré dans l'admin seulement
- **P1** · Manuel
- **Étapes** : ouvrir `/admin/dashboard`, inspecter `<head>` ; naviguer vers la vitrine (`/`) sans recharger.
- **Attendu** : dans l'admin : `<link rel="manifest" href="/admin.webmanifest">` présent ; sur la vitrine des clients : **absent**
  (aucune invite d'installation sur la boutique publique).
- **Statut** : À faire

## PWA-02 — Manifeste valide
- **P1** · Manuel
- **Étapes** : Chrome → DevTools → Application → Manifest.
- **Attendu** : nom « Get STORE — Administration », `start_url` `/admin?source=pwa`, `scope` `/admin`, `display`
  standalone, 3 icônes (192, 512, maskable 512) **chargées sans erreur** ; aucun avertissement bloquant d'installation.
- **Statut** : À faire

## PWA-03 — Service worker actif sans cache
- **P1** · Manuel
- **Attendu** : Application → Service Workers : `admin-sw.js` « activated and running », portée `/admin` ; Cache Storage
  **vide** ; après modification d'une commande dans un autre onglet, rafraîchir l'admin montre la donnée à jour (aucune
  donnée périmée).
- **Statut** : À faire

## PWA-04 — Installation sur Android
- **P1** · Manuel (Chrome Android)
- **Étapes** : `/admin` → menu → « Installer l'application » (ou invite d'installation).
- **Attendu** : icône « Get STORE » (sac « G » sur fond sombre, **non coupée** grâce à l'icône maskable) sur l'écran
  d'accueil ; l'ouverture lance l'admin plein écran sur `/admin`, session conservée.
- **Statut** : À faire

## PWA-05 — Installation sur ordinateur
- **P3** · Manuel (Chrome/Edge)
- **Attendu** : icône d'installation dans la barre d'adresse sur `/admin` ; fenêtre dédiée sans barre d'adresse.
- **Statut** : À faire

## PWA-06 — Navigation hors portée
- **P2** · Manuel
- **Étapes** : dans l'application installée, cliquer « Voir la boutique » (hors `/admin`).
- **Attendu** : la vitrine s'ouvre dans le navigateur (ou une fenêtre intégrée selon la plateforme), pas dans une
  coquille cassée ; le retour à l'admin fonctionne.
- **Statut** : À faire

## PWA-07 — Envois de formulaires et téléversements
- **P1** · Manuel
- **Étapes** : créer un produit avec photo, envoyer un fichier, enregistrer des réglages **dans l'application installée**.
- **Attendu** : tout fonctionne (les requêtes non-GET ne sont pas interceptées par le service worker).
- **Statut** : À faire

## PWA-08 — Pas de régression sur la vitrine
- **P1** · Manuel
- **Attendu** : la vitrine publique ne passe pas par le service worker ; ses performances et ses pages sont inchangées ;
  le panier et le paiement fonctionnent.
- **Statut** : À faire

## PWA-09 — Désinstallation / désenregistrement
- **P3** · Manuel
- **Attendu** : désinstaller l'application ne laisse aucun service worker actif sur la vitrine ; l'admin continue de
  fonctionner dans le navigateur.
- **Statut** : À faire

## PWA-10 — Mise à jour du service worker
- **P3** · Manuel
- **Attendu** : après publication d'une nouvelle version du front, l'admin se met à jour au rechargement (le service
  worker ne retient rien) ; pas besoin de vider le cache.
- **Statut** : À faire

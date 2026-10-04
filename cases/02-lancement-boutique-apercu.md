# 02 — Lancement de boutique et aperçu

Règle : une boutique créée par **inscription publique** est **invisible des clients** tant qu'elle n'est pas lancée.
Les boutiques existantes (migration `V48`, défaut `TRUE`) et celles créées par le Super Admin restent en ligne.

Technique : colonne `fournisseurs.storefront_live` ; garde dans `FournisseurService.resolveAccessibleFournisseur` ;
clé d'aperçu = HMAC du `fournisseurId` (32 caractères hexadécimaux) lue dans l'en-tête `X-Preview-Key` ;
API `PUT /store-settings/me/launch` (rôle ADMIN) corps `{"live": true|false}`.

---

## LAN-01 — Boutique neuve invisible des clients
- **Priorité** : P1 · **Type** : Auto (test d'intégration) + Manuel
- **Préconditions** : boutique créée par inscription publique, jamais lancée.
- **Étapes** : en navigation privée, ouvrir l'adresse de la boutique ; appeler `GET /platform/store?slug=<slug>`.
- **Résultat attendu** : HTTP 403 « Cette boutique ouvre bientôt » ; la vitrine affiche l'écran de boutique fermée,
  jamais les produits.
- **Statut** : À faire

## LAN-02 — Bannière de lancement sur le tableau de bord
- **Priorité** : P1 · **Type** : Manuel
- **Étapes** : se connecter en admin sur la boutique neuve, ouvrir `/admin/dashboard`.
- **Résultat attendu** : une bannière « Votre boutique n'est pas encore visible des clients » avec **Prévisualiser** et
  **Lancer ma boutique**. Elle n'apparaît pas sur une boutique déjà lancée ni sur une boutique ancienne.
- **Statut** : À faire

## LAN-03 — Prévisualiser avant le lancement
- **Priorité** : P1 · **Type** : Manuel
- **Étapes** : cliquer « Prévisualiser » (nouvel onglet, adresse avec `?preview=<clé>`).
- **Résultat attendu** : la vitrine s'affiche normalement dans cet onglet (toutes les pages, pas seulement l'accueil,
  car la clé est gardée pour la session). Dans une **autre** fenêtre privée, sans la clé : toujours 403.
- **Statut** : À faire

## LAN-04 — Clé d'aperçu invalide ou modifiée
- **Priorité** : P1 · **Type** : Auto
- **Étapes** : appeler `GET /platform/store` avec `X-Preview-Key: 00000000000000000000000000000000`, puis avec la clé
  d'une **autre** boutique.
- **Résultat attendu** : 403 dans les deux cas. Le paramètre `?preview=` mal formé (non hexadécimal) est ignoré.
- **Statut** : À faire

## LAN-05 — Lancer la boutique
- **Priorité** : P1 · **Type** : Manuel / Auto
- **Étapes** : cliquer « Lancer ma boutique ».
- **Résultat attendu** : toast « Votre boutique est en ligne 🎉 », la bannière disparaît, la vitrine devient visible en
  navigation privée **sans** clé.
- **Statut** : À faire

## LAN-06 — « Publier ma boutique » lance aussi la boutique
- **Priorité** : P1 · **Type** : Manuel
- **Étapes** : terminer l'assistant de création jusqu'à l'étape Publication, cliquer « Publier ma boutique ».
- **Résultat attendu** : page d'accueil et pages légales créées **et** boutique visible des clients. Si la création de
  la page d'accueil échoue, la boutique n'est **pas** lancée (message d'échec, rien n'est perdu).
- **Statut** : À faire

## LAN-07 — Remettre en préparation
- **Priorité** : P3 · **Type** : Auto
- **Étapes** : `PUT /store-settings/me/launch` avec `{"live": false}` en tant qu'ADMIN.
- **Résultat attendu** : `storefrontLive=false` dans le résumé ; la vitrine redevient 403 pour les clients ; la bannière
  réapparaît.
- **Statut** : À faire

## LAN-08 — Droits sur l'endpoint de lancement
- **Priorité** : P1 · **Type** : Auto
- **Étapes** : appeler `PUT /store-settings/me/launch` (a) sans jeton, (b) avec un compte STAFF, (c) avec un ADMIN
  d'une autre boutique.
- **Résultat attendu** : (a) 401, (b) 403, (c) n'agit que sur **sa** boutique (le tenant vient du jeton, pas du corps).
- **Statut** : À faire

## LAN-09 — Boutiques existantes non touchées
- **Priorité** : P1 · **Type** : Manuel
- **Préconditions** : boutique créée **avant** la migration.
- **Résultat attendu** : après la mise en production, la vitrine reste visible, aucune bannière de lancement.
- **Statut** : À faire

## LAN-10 — Boutique créée par le Super Admin
- **Priorité** : P2 · **Type** : Manuel
- **Étapes** : créer une boutique depuis l'interface Super Admin.
- **Résultat attendu** : statut ACTIF et vitrine **visible** immédiatement (pas de lancement requis).
- **Statut** : À faire

## LAN-11 — Statut de la boutique prioritaire
- **Priorité** : P1 · **Type** : Auto (test d'intégration existant)
- **Étapes** : lancer une boutique puis faire expirer son essai (`expireTrials`).
- **Résultat attendu** : 403 « temporairement indisponible » (statut PENDING), **même lancée** ; après prolongation de
  l'essai par le Super Admin, la vitrine redevient accessible.
- **Statut** : À faire

## LAN-12 — Visiteur déjà connecté en admin sur le même navigateur
- **Priorité** : P3 · **Type** : Manuel
- **Étapes** : ouvrir la vitrine d'une boutique non lancée **sans** clé, mais avec une session admin de cette boutique
  sur la **même origine** (domaine commun).
- **Résultat attendu** : la vitrine s'affiche (administrateur de cette boutique). Avec une session admin d'une
  **autre** boutique : 403.
- **Remarque** : sur sous-domaine distinct, le jeton admin n'est pas partagé : c'est la clé d'aperçu qui sert.
- **Statut** : À faire

## LAN-13 — Limite connue : « Voir la boutique » de l'en-tête
- **Priorité** : P2 · **Type** : Manuel
- **Étapes** : sur boutique non lancée, cliquer « Voir la boutique » dans l'en-tête de l'admin.
- **Résultat attendu (actuel)** : l'onglet ouvert affiche « ouvre bientôt » (la clé d'aperçu n'est ajoutée que par la
  bannière du tableau de bord). **À corriger** si on veut ce lien utilisable avant lancement.
- **Statut** : À faire

# 03 — Tableau de bord : liste « Premiers pas » et accès à l'assistant

Page : `/admin/dashboard`. La liste « Premiers pas » est calculée à partir des données réelles de la boutique.
Un événement navigateur (`troco:assistant-flow`) permet d'ouvrir le chat et de lancer un parcours.

---

## DAS-01 — Étapes affichées
- **Priorité** : P1 · **Type** : Manuel
- **Préconditions** : boutique neuve sans logo, sans contact, sans produit, sans transporteur, sans pages légales.
- **Résultat attendu** : 5 étapes à faire (logo, contact, premier produit réel, livraison, pages légales) + « voir ma
  boutique » ; barre de progression 0/6 ; le compteur « il reste N étapes » est juste.
- **Statut** : À faire

## DAS-02 — Les étapes se cochent d'elles-mêmes
- **Priorité** : P1 · **Type** : Manuel
- **Étapes** : ajouter un logo ; renseigner un téléphone **ou** un WhatsApp ; créer un transporteur ; créer les pages
  légales ; créer un produit réel.
- **Résultat attendu** : chaque ligne passe en coche verte au rechargement ; les **produits d'exemple** (SKU `DEMO-…`)
  ne comptent **pas** comme « premier produit réel » ; « voir ma boutique » n'est cochée qu'avec logo **et** un vrai
  produit.
- **Statut** : À faire

## DAS-03 — Bouton « Avec l'assistant »
- **Priorité** : P1 · **Type** : Manuel
- **Préconditions** : `AI_ASSISTANT_ENABLED=true`.
- **Étapes** : cliquer « Avec l'assistant » sur chaque ligne non faite.
- **Résultat attendu** :
  | Ligne | Parcours lancé |
  |---|---|
  | Logo / Contact | « Configurer ma boutique » (basics) |
  | Premier produit | Catalogue |
  | Livraison | Livraison |
  | Pages légales | Conformité |
  Le chat s'ouvre et la première question du parcours est posée, sans autre clic.
- **Statut** : À faire

## DAS-04 — Assistant désactivé
- **Priorité** : P2 · **Type** : Manuel
- **Préconditions** : `AI_ASSISTANT_ENABLED=false`.
- **Résultat attendu** : aucun bouton « Avec l'assistant », aucun bouton flottant ; la liste reste utilisable via les
  liens des lignes.
- **Statut** : À faire

## DAS-05 — Ligne faite : pas de bouton
- **Priorité** : P3 · **Type** : Manuel
- **Résultat attendu** : une étape cochée n'affiche pas « Avec l'assistant ».
- **Statut** : À faire

## DAS-06 — Écran d'accueil du chat réduit
- **Priorité** : P2 · **Type** : Manuel
- **Étapes** : ouvrir le chat vide.
- **Résultat attendu** : 4 parcours visibles (basics, catalogue, design, livraison), puis « Plus de parcours » qui
  révèle les 6 autres (marketing, fonctions Pro, conformité, modifier l'existant, contenu, paiements) et devient
  « Moins de parcours ».
- **Statut** : À faire

## DAS-07 — Pastille de rappel sur le bouton du chat
- **Priorité** : P2 · **Type** : Manuel
- **Préconditions** : boutique avec des étapes de base non faites (hors couleurs et WhatsApp Business).
- **Résultat attendu** : pastille rouge avec le nombre d'étapes (info-bulle « N étape(s) de configuration à
  terminer ») ; elle disparaît dès l'ouverture du chat, **pour la session** ; elle ne revient pas après un changement de
  page, mais revient dans un nouvel onglet/session.
- **Statut** : À faire

## DAS-08 — Pastille absente quand tout est fait
- **Priorité** : P3 · **Type** : Manuel
- **Résultat attendu** : aucune pastille si toutes les étapes de base sont faites.
- **Statut** : À faire

## DAS-09 — Lancement depuis un autre écran pendant une conversation
- **Priorité** : P3 · **Type** : Manuel
- **Étapes** : avoir une conversation en cours dans le chat, cliquer « Avec l'assistant » sur le tableau de bord.
- **Résultat attendu** : le parcours demandé démarre à la suite de la conversation (l'historique n'est pas perdu) ;
  un seul outil actif à la fois.
- **Statut** : À faire

## DAS-10 — Le tour guidé en fenêtre
- **Priorité** : P3 · **Type** : Manuel
- **Résultat attendu** : le guide de première utilisation s'ouvre **une seule fois** après l'assistant de création,
  jamais pendant `/admin/onboarding`, et ne se rouvre pas à la reconnexion.
- **Statut** : À faire

# 01 — Inscription et création de boutique

Page : `/creer-boutique` (composant `CreateStore`). API : `POST /platform/register`, `GET /platform/slug-check`.
Après succès : connexion automatique puis redirection vers `/admin/onboarding`.

Légende statut : `À faire` · `OK` · `KO`.

---

## INS-01 — Parcours nominal en trois champs
- **Priorité** : P1 · **Type** : Manuel (+ test vitest existant)
- **Préconditions** : visiteur non connecté, page ouverte en français.
- **Étapes**
  1. Saisir le nom « Maison Atlas ».
  2. Saisir un email jamais utilisé et un mot de passe de 8 caractères ou plus.
  3. Cliquer « Lancer ma boutique ».
- **Résultat attendu** : la page n'affiche que nom, secteur, email, mot de passe et la ligne « Adresse de votre
  boutique » ; la boutique est créée, vous êtes connecté et redirigé vers `/admin/onboarding` ; message « essai gratuit
  de 30 jours démarré ».
- **Statut** : À faire

## INS-02 — L'adresse se déduit du nom
- **Priorité** : P1 · **Type** : Manuel
- **Étapes** : saisir « Maison Atlas ».
- **Résultat attendu** : la ligne d'adresse affiche l'URL de la boutique avec `maison-atlas` (sans accents, tirets) ;
  le champ d'adresse n'est **pas** visible tant qu'on ne clique pas « Modifier ».
- **Statut** : À faire

## INS-03 — Modifier l'adresse
- **Priorité** : P2 · **Type** : Manuel
- **Étapes** : cliquer « Modifier », taper « Ma Boutique_2 ».
- **Résultat attendu** : le champ nettoie la saisie en direct (minuscules, `-` à la place des espaces et `_`, pas de
  caractères spéciaux) ; l'aperçu de l'URL se met à jour ; une fois le nom modifié à la main, changer le nom de la
  boutique **ne réécrit plus** l'adresse.
- **Statut** : À faire

## INS-04 — Adresse déjà prise
- **Priorité** : P1 · **Type** : Manuel
- **Préconditions** : la boutique `gold-yara` existe.
- **Étapes** : saisir le nom « Gold Yara ».
- **Résultat attendu** : après ~0,4 s l'icône passe au rouge, le champ d'adresse **s'ouvre tout seul**, le message
  « Cette adresse est déjà prise » apparaît avec un bouton « Utiliser « … » » qui applique la suggestion ; envoyer le
  formulaire sans corriger est refusé.
- **Statut** : À faire

## INS-05 — Nom en arabe (adresse de secours)
- **Priorité** : P2 · **Type** : Manuel
- **Étapes** : saisir le nom « متجر الأطلس ».
- **Résultat attendu** : une adresse de secours de la forme `boutique-xxxx` est proposée et modifiable ; l'inscription
  aboutit.
- **Statut** : À faire

## INS-06 — Validations des champs
- **Priorité** : P1 · **Type** : Manuel / Auto (vitest)
- **Étapes** : envoyer le formulaire (a) vide, (b) email `abc`, (c) mot de passe de 5 caractères.
- **Résultat attendu** : un toast « Corrigez les champs indiqués en rouge », chaque champ fautif porte un message sous
  lui, le focus va au premier champ en erreur, **aucun appel** `registerStore` n'est émis.
- **Statut** : À faire

## INS-07 — Mot de passe affichable
- **Priorité** : P3 · **Type** : Manuel
- **Étapes** : cliquer l'œil du champ mot de passe.
- **Résultat attendu** : le texte devient visible, le libellé du bouton bascule (« Afficher / Masquer le mot de passe »).
- **Statut** : À faire

## INS-08 — Secteur facultatif et préremplissage de l'assistant
- **Priorité** : P2 · **Type** : Manuel
- **Étapes** : choisir « Mode & vêtements » (la pastille est « enfoncée », recliquer la désélectionne), terminer
  l'inscription.
- **Résultat attendu** : l'assistant de création (`/admin/onboarding`) propose le thème conseillé du secteur (Élégant
  pour la mode) et les produits d'exemple correspondants. Sans secteur choisi : parcours normal sans pack.
- **Statut** : À faire

## INS-09 — Options repliées
- **Priorité** : P3 · **Type** : Manuel
- **Étapes** : cliquer « Plus d'options ».
- **Résultat attendu** : apparaissent « Votre nom (optionnel) » et « Téléphone / WhatsApp (optionnel) » ; le bouton
  devient « Moins d'options » (`aria-expanded` à jour). Les valeurs saisies sont envoyées à l'inscription.
- **Statut** : À faire

## INS-10 — Plan transmis par l'adresse
- **Priorité** : P2 · **Type** : Manuel
- **Étapes** : ouvrir `/creer-boutique?plan=pro`, terminer l'inscription.
- **Résultat attendu** : la boutique est créée sur le plan Pro ; sous le bouton, le texte « 30 jours d'essai gratuit,
  puis plan Pro ». Sans paramètre : plan Basic. Si la liste des plans est indisponible : « puis le plan choisi ».
- **Statut** : À faire

## INS-11 — Email déjà utilisé
- **Priorité** : P1 · **Type** : Manuel
- **Étapes** : s'inscrire avec l'email d'un compte existant.
- **Résultat attendu** : toast d'erreur du serveur (« Cet email admin est déjà utilisé »), le formulaire reste rempli,
  le bouton redevient actif.
- **Statut** : À faire

## INS-12 — Connexion automatique impossible
- **Priorité** : P3 · **Type** : Manuel (simulé)
- **Étapes** : faire échouer la connexion juste après la création (ex. coupure réseau après `register`).
- **Résultat attendu** : toast « Boutique créée. Connectez-vous avec <email> » et redirection vers `/admin` avec
  l'email prérempli.
- **Statut** : À faire

## INS-13 — Activation manuelle (essai désactivé)
- **Priorité** : P3 · **Type** : Manuel
- **Préconditions** : `APP_TRIAL_DAYS=0`.
- **Résultat attendu** : message « en attente d'activation Get STORE » et bannière d'attente sur le tableau de bord.
- **Statut** : À faire

## INS-14 — Double envoi
- **Priorité** : P2 · **Type** : Manuel
- **Étapes** : double-cliquer rapidement sur « Lancer ma boutique ».
- **Résultat attendu** : un seul appel à l'API ; le bouton affiche « Création… » et est désactivé.
- **Statut** : À faire

## INS-15 — Réseau lent sur la vérification d'adresse
- **Priorité** : P3 · **Type** : Manuel
- **Étapes** : bloquer `GET /platform/slug-check` (hors-ligne), taper un nom.
- **Résultat attendu** : l'icône disparaît, **aucune erreur bloquante** ; l'inscription reste possible (le serveur
  refusera si l'adresse est prise).
- **Statut** : À faire

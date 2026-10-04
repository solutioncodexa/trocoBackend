# 04 — Assistant : les 10 parcours guidés

Principe à vérifier partout : **c'est l'interface qui pose les questions et enregistre** (API d'administration
existantes). Le modèle ne fait que répondre aux questions libres. Chaque parcours :

- ne propose **que ce que le plan autorise** ;
- ne repose pas ce qui est déjà fait (« tout est déjà en place ✅ ») ;
- laisse **passer** chaque étape (bouton « Passer ») ;
- affiche l'outil adapté (champ, pastilles, récapitulatif, téléversement) et n'active **qu'un outil à la fois**
  (le dernier message avec un outil) ;
- en cas d'échec serveur : message d'erreur dans le chat, **l'outil reste affiché**, rien n'est perdu ;
- est disponible en français, anglais, arabe (voir `11-langues-et-rtl.md`).

Préconditions communes : `AI_ASSISTANT_ENABLED=true`, bouton flottant visible, boutique du plan indiqué.

---

## 4.1 Configuration de base (`PAR-BAS`)

Étapes (dans l'ordre, selon ce qui manque) : logo → couleurs → slogan → téléphone → WhatsApp → paiement à la livraison
→ livraison gratuite → WhatsApp Business (**Pro et plus seulement**).

### PAR-BAS-01 — Parcours complet sur boutique vide
- **P1** · Manuel · Plan Pro
- **Étapes** : « Configurer ma boutique », répondre à chaque étape (logo = image, couleurs = une pastille, slogan,
  téléphone `0612345678`, WhatsApp `+212612345678`, paiement à la livraison = Oui, seuil `500`).
- **Attendu** : chaque réponse est **enregistrée** (vérifier dans `/admin/parametres`) ; message de fin ; le logo
  apparaît dans l'en-tête de l'admin ; les couleurs s'appliquent à la vitrine.
- **Statut** : À faire

### PAR-BAS-02 — Plan Basic : pas de WhatsApp Business
- **P1** · Manuel · `test-basic`
- **Attendu** : à aucun moment la question WhatsApp Business n'est posée ; en la demandant en texte libre, l'assistant
  répond que c'est réservé au plan Pro, sans la proposer comme étape.
- **Statut** : À faire

### PAR-BAS-03 — Validations
- **P1** · Manuel/Auto
- **Étapes** : téléphone `abc`, seuil `-5`, seuil `0`, seuil `1 000,50`.
- **Attendu** : `abc` et `-5` → « réponse non valide », l'outil reste ; `1 000,50` et `1000,5` acceptés si > 0 ;
  `0` refusé pour la livraison gratuite.
- **Statut** : À faire

### PAR-BAS-04 — Couleurs : aperçu en direct
- **P2** · Manuel
- **Attendu** : le bouton « Aperçu en direct » ouvre `/admin/parametres?section=identity` **dans un autre onglet** (la
  conversation est conservée) ; la section Identité de l'apparence s'ouvre directement.
- **Statut** : À faire

### PAR-BAS-05 — Passer toutes les étapes
- **P3** · Manuel
- **Attendu** : rien n'est modifié, le message de fin s'affiche.
- **Statut** : À faire

---

## 4.2 Catalogue (`PAR-CAT`)

Étapes : activité (modèle d'arborescence) → catégories / sous-catégories → produits (nom, prix, photos, description,
stock, confirmation).

### PAR-CAT-01 — Arborescence depuis un modèle
- **P1** · Manuel
- **Étapes** : choisir une activité (ex. mode), cocher/décocher des catégories et sous-catégories, valider.
- **Attendu** : seules les catégories cochées sont créées, les sous-catégories sous le bon parent ; relancer le parcours
  **ne crée pas de doublons** ; l'arborescence est affichée dans la langue de l'interface.
- **Statut** : À faire

### PAR-CAT-02 — Produit complet avec photos
- **P1** · Manuel
- **Étapes** : nom, prix `149`, 2 photos, description proposée (« Utiliser »), stock `20`, confirmer.
- **Attendu** : produit créé dans la bonne catégorie, 2 photos (compressées), description et stock enregistrés ;
  proposition de produit suivant.
- **Statut** : À faire

### PAR-CAT-03 — Produit **sans photo**
- **P1** · Manuel
- **Étapes** : au moment des photos, cliquer « Passer ».
- **Attendu** : le produit est créé (le serveur accepte), la vitrine affiche l'**image par défaut** ; ce cas **échouait**
  avant la correction.
- **Statut** : À faire

### PAR-CAT-04 — Photos invalides
- **P2** · Manuel
- **Étapes** : choisir un fichier non image, puis plus de photos que la limite.
- **Attendu** : message « photos non valides », l'outil reste, aucun produit créé.
- **Statut** : À faire

### PAR-CAT-05 — Limite de produits du plan
- **P2** · Manuel
- **Préconditions** : plan dont `maxProducts` est atteint.
- **Attendu** : message d'erreur du serveur dans le chat (pas de plantage) ; l'assistant ne propose pas d'autre produit.
- **Statut** : À faire

### PAR-CAT-06 — Prix et stock invalides
- **P1** · Auto (logique pure)
- **Étapes** : prix `0`, `abc`, `1e9` ; stock `-1`, `1.5`.
- **Attendu** : refusés ; `149,90` accepté pour le prix ; le stock accepte les entiers ≥ 0.
- **Statut** : À faire

---

## 4.3 Design (`PAR-DES`)

Étapes : thème → disposition de l'en-tête → pages (modèles) → réseaux sociaux → bandeau promo → pages légales.
*(Couvre les 3 tests vitest mis en pause.)*

### PAR-DES-01 — Thème autorisé par le plan
- **P1** · Manuel
- **Étapes** : plan Basic → carte « Élégant ».
- **Attendu** : les thèmes non inclus portent une **couronne** « Pro » ; cliquer dessus **n'applique pas** le thème et
  explique comment l'obtenir (/admin/reglages) ; « Classique » et « Minimal » s'appliquent. En Pro : les 4 s'appliquent.
- **Statut** : À faire

### PAR-DES-02 — Application du thème
- **P1** · Manuel
- **Attendu** : le thème choisi est enregistré, la vitrine change, l'ancien look est conservé (retour possible).
- **Statut** : À faire

### PAR-DES-03 — Réseaux sociaux
- **P2** · Auto/Manuel
- **Étapes** : `instagram.com/maboutique`, `@maboutique`, `https://facebook.com/x`, `javascript:alert(1)`.
- **Attendu** : les trois premiers sont normalisés en URL `https://…` ; `javascript:` refusé.
- **Statut** : À faire

### PAR-DES-04 — Pages légales
- **P1** · Manuel
- **Attendu** : mentions légales, CGV, retours, confidentialité créées **sans écraser** celles qui existent ; rappel que
  ce sont des **modèles à relire** ; l'URL de la politique de confidentialité est renseignée si vide.
- **Statut** : À faire

### PAR-DES-05 — Bandeau promo
- **P3** · Manuel
- **Attendu** : texte trop long ou vide refusé ; texte valide enregistré et visible en haut de la vitrine.
- **Statut** : À faire

---

## 4.4 Livraison (`PAR-LIV`)

### PAR-LIV-01 — Premier transporteur
- **P1** · Manuel
- **Étapes** : choisir « Amana » (suggestions : Amana, Ozon Express, Cathedis, Aramex, DHL, « Mon propre
  transporteur »), frais `35`, délai `2-4`, confirmer.
- **Attendu** : transporteur créé avec un code unique (`AMANA`, puis `AMANA_2` si doublon), frais 35 MAD, délai 2 à 4 j,
  visible dans `/admin/livraison` et au paiement côté vitrine.
- **Statut** : À faire

### PAR-LIV-02 — Formats de délai
- **P2** · Auto
- **Attendu** : `3`, `2-4`, `2 à 4`, `2–4` acceptés ; `0`, `5-2`, `70`, `abc` refusés.
- **Statut** : À faire

### PAR-LIV-03 — Frais à zéro
- **P2** · Auto
- **Attendu** : `0` accepté (livraison offerte) ; `-1` et `100001` refusés ; `12,5` accepté.
- **Statut** : À faire

### PAR-LIV-04 — Nom invalide
- **P3** · Auto
- **Attendu** : 1 caractère, plus de 60 caractères ou `<script>` refusés.
- **Statut** : À faire

---

## 4.5 Paiements par carte (`PAR-PAY`)

Règle absolue : **aucune clé ou secret ne passe par le modèle ni ne reste dans l'historique/`sessionStorage`**.

### PAR-PAY-01 — Saisie masquée
- **P1** · Manuel
- **Étapes** : choisir Stripe, saisir la clé publique puis la clé secrète.
- **Attendu** : le champ secret est **masqué** ; après envoi le chat n'affiche que `••••••••` ; ouvrir les outils de
  développement → `sessionStorage` (`troco_assistant_chat`) ne contient **aucune** clé.
- **Statut** : À faire

### PAR-PAY-02 — Clé live détectée
- **P1** · Manuel
- **Étapes** : saisir une clé commençant par `sk_live_`.
- **Attendu** : avertissement explicite « clé de production » avant l'enregistrement.
- **Statut** : À faire

### PAR-PAY-03 — Test de connexion
- **P1** · Manuel (clés de test du fournisseur)
- **Attendu** : le test d'identifiants est lancé ; succès → paiement activé ; échec → message clair, **rien
  d'activé**, la clé n'est pas conservée en clair dans le navigateur.
- **Statut** : À faire

### PAR-PAY-04 — Coller une clé dans le champ libre du chat
- **P1** · Manuel
- **Étapes** : taper/coller `sk_live_abcdefghijklmnop1234` dans la zone de message.
- **Attendu** : l'envoi est **bloqué côté navigateur** avec le message « ne saisissez jamais de clé ici » ; rien n'est
  envoyé au serveur. Un jeton `Bearer …` ou une longue chaîne de 40+ caractères est bloqué de la même façon.
- **Statut** : À faire

### PAR-PAY-05 — Filet de sécurité serveur
- **P1** · Auto
- **Étapes** : appeler `POST /assistant/chat` directement avec un message contenant `sk_test_abcdefgh12345678`.
- **Attendu** : le message transmis au fournisseur LLM contient `[clé masquée]` à la place de la clé.
- **Statut** : À faire

### PAR-PAY-06 — Paiement à la livraison
- **P2** · Manuel
- **Attendu** : activer/désactiver est enregistré ; la vitrine propose ou non ce mode au paiement.
- **Statut** : À faire

---

## 4.6 Marketing et référencement (`PAR-MKT`)

### PAR-MKT-01 — Pixels dans la limite du plan
- **P1** · Manuel
- **Étapes** : Basic (1 pixel) : saisir un pixel Meta `123456789012345` ; la suite doit proposer TikTok/Google ?
- **Attendu** : après le 1er pixel, l'assistant explique que le plan autorise **1 pixel** et que la limite est atteinte,
  avec renvoi à l'abonnement ; **ne pose pas** la question suivante. En Business : tous proposés.
- **Statut** : À faire

### PAR-MKT-02 — Formats d'identifiants
- **P1** · Auto
- **Attendu** : Meta = 15 ou 16 chiffres ; Google `G-ABC123DEF4` (Analytics) ou `AW-123456789` (Ads) ; autre → refus.
  Google compte pour **un seul** emplacement (Analytics ou Ads), comme le serveur.
- **Statut** : À faire

### PAR-MKT-03 — Titre/description Google de l'accueil
- **P1** · Manuel
- **Attendu** : proposition (≤ 60 et ≤ 155 caractères) dans la langue de l'interface ; « Appliquer » enregistre sur la
  page d'accueil **sans écraser** les autres champs (publication, dates, A/B) ; un titre déjà rempli n'est pas touché.
- **Statut** : À faire

### PAR-MKT-04 — SEO des catégories en lot
- **P2** · Manuel
- **Préconditions** : 3 catégories sans titre/description.
- **Attendu** : proposé « pour toutes » ; seuls les **champs vides** sont remplis (30 catégories maximum par passage) ;
  les champs déjà remplis sont conservés.
- **Statut** : À faire

### PAR-MKT-05 — Premier code promo
- **P1** · Manuel
- **Étapes** : code `bienvenue10` → pourcentage → `10` → utilisations `100` → récapitulatif → créer.
- **Attendu** : code créé en **MAJUSCULES** `BIENVENUE10`, 10 %, 100 utilisations ; pourcentage `0` ou `95` refusés ;
  montant fixe accepté jusqu'à 100 000 ; code de 2 caractères ou avec espaces/symboles refusé. L'étape n'est proposée
  **que s'il n'existe aucun code**.
- **Statut** : À faire

---

## 4.7 Fonctions Pro (`PAR-PRO`)

### PAR-PRO-01 — Basic : rien de payant proposé
- **P1** · Manuel · `test-basic`
- **Attendu** : le parcours répond une ligne « inclus dans les plans supérieurs » et renvoie à l'abonnement ; aucune
  question sur paniers abandonnés, WhatsApp Business, fidélité ou domaine.
- **Statut** : À faire

### PAR-PRO-02 — Paniers abandonnés
- **P1** · Manuel · `test-pro` avec relance **désactivée**
- **Étapes** : oui → délai 3 h.
- **Attendu** : relance activée avec 180 minutes (vérifier `/admin/paniers-abandonnes` et les réglages). Si la relance
  est déjà activée : l'étape n'est pas proposée.
- **Statut** : À faire

### PAR-PRO-03 — Message WhatsApp de commande
- **P2** · Manuel
- **Préconditions** : un numéro WhatsApp renseigné, modèle vide.
- **Attendu** : 2 modèles proposés dans la langue de l'interface ; celui choisi est enregistré tel quel avec
  `{productName}` et `{url}` ; **sans numéro WhatsApp** : l'étape n'est pas proposée.
- **Statut** : À faire

### PAR-PRO-04 — Fidélité
- **P2** · Manuel
- **Étapes** : points par MAD = 2, valeur d'un point = 0,10, activer.
- **Attendu** : programme activé avec ces valeurs ; l'étape n'apparaît pas s'il est déjà actif.
- **Statut** : À faire

### PAR-PRO-05 — Domaine personnalisé
- **P1** · Manuel
- **Étapes** : saisir `https://MaBoutique.ma/` ; puis `getstore.com` ; puis `abc`.
- **Attendu** : le 1er est nettoyé en `maboutique.ma` et enregistré ; les domaines de la plateforme (`…getstore.com`,
  `…codexa-solution.com`) et les saisies sans point sont **refusés** ; le chat donne l'hôte et la cible CNAME
  (`<slug>.getstore.com`) ; « Vérifier » renvoie « vérifié ✅ » ou « propagation, jusqu'à 24 h » sans bloquer. Domaine
  déjà pris par une autre boutique → erreur du serveur affichée.
- **Statut** : À faire

---

## 4.8 Conformité (`PAR-CNP`)

### PAR-CNP-01 — Pages légales manquantes
- **P1** · Manuel
- **Attendu** : proposée seulement si aucune page de confidentialité (ni lien externe) ; crée les 4 pages sans écraser ;
  rappel « modèles à relire ».
- **Statut** : À faire

### PAR-CNP-02 — Consentement aux cookies
- **P1** · Manuel
- **Préconditions** : un pixel renseigné **et** bandeau de consentement désactivé.
- **Attendu** : recommandation d'activer ; « Oui » active le bandeau (visible côté vitrine). Sans pixel, ou bandeau
  déjà actif : étape non proposée.
- **Statut** : À faire

### PAR-CNP-03 — Durée de conservation
- **P2** · Manuel
- **Étapes** : choisir « 2 an(s) ».
- **Attendu** : `dataRetentionDays=730` et version CNDP `1` enregistrées ; l'étape n'est plus proposée ensuite.
  **Aucune purge automatique n'existe** : la durée est un réglage affiché (à ne pas présenter comme une purge).
- **Statut** : À faire

---

## 4.9 Modifier ou supprimer l'existant (`PAR-MOD`)

### PAR-MOD-01 — Recherche de produit
- **P1** · Manuel
- **Étapes** : « Un produit », taper `t-shi` (sans accent / en minuscules).
- **Attendu** : trouve « T-shirt coton » ; plusieurs résultats → liste de 8 choix maximum ; aucun résultat → message
  « rien trouvé » et le champ reste.
- **Statut** : À faire

### PAR-MOD-02 — Changer le prix et le stock
- **P1** · Manuel
- **Attendu** : prix `199` appliqué tout de suite avec « ancien → nouveau » ; stock `5` idem ; le prix d'origine barré
  n'est conservé que s'il reste **supérieur** au nouveau prix ; photos, SKU, description inchangés.
- **Statut** : À faire

### PAR-MOD-03 — Produit à variantes
- **P1** · Manuel
- **Préconditions** : produit avec taille/couleur.
- **Attendu** : l'assistant **refuse** de modifier prix/stock et renvoie vers `/admin/produits` (rien n'est écrasé).
- **Statut** : À faire

### PAR-MOD-04 — Renommer / masquer une catégorie
- **P2** · Manuel
- **Attendu** : le nom change mais le **slug reste identique** (liens existants intacts) ; « Masquer » la rend invisible
  sur la vitrine, « Réafficher » inverse ; le bouton proposé dépend de l'état actuel.
- **Statut** : À faire

### PAR-MOD-05 — Suppression avec confirmation
- **P1** · Manuel
- **Étapes** : « Supprimer » un produit, puis une catégorie.
- **Attendu** : récapitulatif (nom, prix / nombre de produits), boutons « Oui, supprimer » / « Annuler » ; **rien n'est
  supprimé sans clic** ; annuler → « rien n'a été modifié » ; confirmer → disparu de la vitrine.
- **Statut** : À faire

### PAR-MOD-06 — Suppression refusée par le serveur
- **P2** · Manuel
- **Préconditions** : catégorie contenant des produits ou sous-catégories (comportement serveur à constater).
- **Attendu** : le message d'erreur du serveur s'affiche dans le chat ; rien ne casse.
- **Statut** : À faire

---

## 4.10 Contenu de base (`PAR-CNT`)

### PAR-CNT-01 — Email, ville, présentation
- **P1** · Manuel
- **Attendu** : email invalide refusé ; ville < 2 caractères refusée ; présentation < 20 caractères refusée ;
  la présentation est **proposée** (modifiable) ; chaque valeur est enregistrée.
- **Statut** : À faire

### PAR-CNT-02 — Page FAQ
- **P1** · Manuel
- **Attendu** : page `faq` créée, publiée, **ajoutée au menu** ; ses 4 réponses reflètent les réglages réels
  (paiements activés, seuil de livraison gratuite, contacts) et **aucune politique de retour inventée** ; non recréée si
  elle existe déjà (`faq`, `questions-frequentes`, `foire-aux-questions`).
- **Statut** : À faire

### PAR-CNT-03 — Page Contact
- **P2** · Manuel
- **Attendu** : page `contact` avec formulaire ; un message envoyé depuis la vitrine arrive dans `/admin/leads` ;
  non recréée si elle existe déjà.
- **Statut** : À faire

### PAR-CNT-04 — Équipe
- **P2** · Manuel
- **Attendu** : à la fin, l'assistant renvoie vers `/admin/membres` et **ne demande jamais de mot de passe** de
  collaborateur dans le chat.
- **Statut** : À faire

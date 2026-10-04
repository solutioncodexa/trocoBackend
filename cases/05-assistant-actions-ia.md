# 05 — Assistant : actions du modèle (appels de fonctions)

Active **uniquement en conversation libre** (pas pendant un parcours guidé) et si `AI_TOOLS_ENABLED=true`.
Le **code** décide : le modèle ne peut ni contourner un niveau, ni un droit, ni un plafond.

| Outil | Niveau | Droit requis | Annulable |
|---|---|---|---|
| `get_store_state`, `list_categories` | Lecture, immédiat | — | — |
| `list_products` | Lecture, immédiat | `PRODUCTS_VIEW` | — |
| `update_store_texts` (slogan, présentation) | Auto, journalisé | ADMIN | Oui |
| `update_contact` (email, téléphone, WhatsApp, ville) | Auto, journalisé | ADMIN | Oui |
| `set_free_shipping_threshold` | Auto, journalisé | ADMIN | Oui si un seuil existait |
| `create_category` | Auto, journalisé | `CATALOG_MANAGE` | Oui (supprime la catégorie) |
| `delete_category` | **Confirmation** | `CATALOG_MANAGE` | Non |
| `delete_product` | **Confirmation** | `PRODUCTS_DELETE` | Non |
| `set_theme` | **Confirmation** | ADMIN | Oui |
| `create_promo_code` | **Confirmation** | ADMIN | Oui (supprime le code) |

**Hors périmètre, sans outil** : création de produit (photos), clés de paiement, mots de passe, abonnement,
collaborateurs.

Plafonds : 4 allers-retours et 5 écritures par message ; 40 écritures par jour et par boutique ; confirmation valable
10 minutes ; annulation valable 30 minutes (en mémoire : un redémarrage du serveur les efface).

Préconditions communes : `AI_ASSISTANT_ENABLED=true`, `AI_TOOLS_ENABLED=true`, **chat vide ou conversation libre**.

---

## ACT-01 — Premier appel réel avec Gemini
- **P1** · Manuel (bloquant avant tout le reste)
- **Étapes** : écrire « mets mon slogan à "Le meilleur du Maroc" ».
- **Attendu** : le modèle appelle `update_store_texts` ; message « Textes de la boutique mis à jour ✅ » puis phrase du
  modèle ; le slogan change dans `/admin/parametres`. **Si erreur** : consulter les journaux (`assistant_turn`, avertissement
  « Assistant LLM indisponible ») ; points à vérifier : nom du modèle, signature des appels de fonction renvoyée.
- **Statut** : À faire

## ACT-02 — Lecture : « que me reste-t-il à faire ? »
- **P1** · Manuel
- **Attendu** : le modèle appelle `get_store_state`, annonce les manques (logo, slogan, etc.) **sans rien modifier**, et
  propose **une** étape ; aucune action affichée dans le chat.
- **Statut** : À faire

## ACT-03 — Écriture automatique + annulation
- **P1** · Manuel
- **Étapes** : « mon email de contact est contact@maboutique.ma » → cliquer « Annuler cette action ».
- **Attendu** : « Coordonnées mises à jour ✅ » avec un bouton « Annuler cette action » ; après clic : « Action annulée,
  la boutique est revenue à l'état précédent ✅ » et l'**ancien email est rétabli** ; recliquer n'est plus possible
  (usage unique).
- **Statut** : À faire

## ACT-04 — Valeur non inventée
- **P1** · Manuel
- **Étapes** : « mets mon téléphone » (sans numéro).
- **Attendu** : l'assistant **demande le numéro** ; aucun outil n'est appelé avec une valeur inventée.
- **Statut** : À faire

## ACT-05 — Arguments invalides refusés par le code
- **P1** · Manuel
- **Étapes** : « mon email est abc », « mets la livraison gratuite à 0 », « seuil à -50 ».
- **Attendu** : message « L'action n'a pas pu être faite : … » (email invalide / montant hors limites), rien d'écrit.
- **Statut** : À faire

## ACT-06 — Création de catégorie et sous-catégorie
- **P1** · Manuel
- **Étapes** : « crée la catégorie Chaussures », puis « ajoute Baskets dedans ».
- **Attendu** : les deux sont créées (la 2e avec le bon parent) ; un nom arabe produit une adresse `categorie-xxxxxx` ;
  un nom déjà pris produit un suffixe plutôt qu'un échec ; annulation = suppression de la catégorie créée.
- **Statut** : À faire

## ACT-07 — Suppression : confirmation obligatoire
- **P1** · Manuel
- **Étapes** : « supprime la catégorie Chaussures ».
- **Attendu** : le modèle ne supprime **pas** ; une carte « Supprimer la catégorie « Chaussures » ? » avec « Oui,
  confirmer » / « Annuler » apparaît ; la catégorie existe toujours tant qu'on n'a pas confirmé ; confirmer la supprime ;
  annuler affiche « Action annulée, rien n'a été modifié ».
- **Statut** : À faire

## ACT-08 — Confirmation expirée
- **P1** · Manuel
- **Étapes** : demander une suppression, attendre > 10 minutes (ou redémarrer le backend), confirmer.
- **Attendu** : « Cette action a expiré. Redemandez-la-moi » ; **rien n'est supprimé**.
- **Statut** : À faire

## ACT-09 — Une seule confirmation à la fois
- **P2** · Manuel
- **Étapes** : « supprime les catégories A et B ».
- **Attendu** : une seule carte de confirmation ; le modèle est informé qu'il doit attendre ; la seconde demande se
  fait après la première.
- **Statut** : À faire

## ACT-10 — Changement de thème
- **P2** · Manuel
- **Étapes** : « passe au thème bold » (plan Basic), puis (plan Pro).
- **Attendu** : carte de confirmation « L'apparence va changer » ; Basic : après confirmation, le **serveur refuse**
  (thème non inclus) et le message d'erreur s'affiche ; Pro : appliqué, annulation possible (retour à l'ancien thème).
- **Statut** : À faire

## ACT-11 — Code promo proposé, jamais créé sans accord
- **P1** · Manuel
- **Étapes** : « crée un code BIENVENUE10 de 10 % ».
- **Attendu** : carte « Créer le code promo « BIENVENUE10 » (10 %) ? » ; rien n'existe avant confirmation ; code déjà
  existant → erreur « Ce code promo existe déjà » ; annulation = suppression du code.
- **Statut** : À faire

## ACT-12 — Création de produit demandée
- **P2** · Manuel
- **Étapes** : « ajoute un produit T-shirt à 149 ».
- **Attendu** : pas d'outil de création de produit : l'assistant renvoie vers le **parcours catalogue** (photos).
- **Statut** : À faire

## ACT-13 — Plafond de 5 écritures par message
- **P2** · Manuel
- **Étapes** : « crée les catégories A, B, C, D, E, F, G ».
- **Attendu** : au plus 5 créées ; le sixième appel reçoit « Limite d'actions atteinte pour ce message » ; les actions
  déjà faites restent affichées.
- **Statut** : À faire

## ACT-14 — Plafond quotidien d'écritures
- **P2** · Auto/Manuel
- **Préconditions** : `AI_TOOLS_DAILY_WRITE_LIMIT=3`.
- **Attendu** : à la 4e écriture : « Limite quotidienne d'actions atteinte » ; les lectures restent possibles ; le compteur
  repart le lendemain.
- **Statut** : À faire

## ACT-15 — Interrupteur d'urgence
- **P1** · Manuel
- **Étapes** : passer `AI_TOOLS_ENABLED=false`, redémarrer, redemander une modification ; essayer aussi de confirmer une
  carte ouverte avant la coupure.
- **Attendu** : le chat répond en **conseil seulement** (« je ne peux pas modifier… ») ; la confirmation d'une action
  restée à l'écran est refusée (« actions désactivées »).
- **Statut** : À faire

## ACT-16 — Pendant un parcours guidé
- **P1** · Manuel
- **Étapes** : démarrer un parcours, poser une question libre au milieu.
- **Attendu** : le modèle **répond brièvement** puis invite à poursuivre ; **aucun outil** n'est appelé.
- **Statut** : À faire

## ACT-17 — Droits d'un compte STAFF
- **P1** · Manuel/Auto
- **Étapes** : STAFF **sans** `CATALOG_MANAGE` : « crée la catégorie Test » ; « change mon slogan ».
- **Attendu** : le modèle ne voit pas ces outils (« je n'ai pas cet outil ») ; aucune écriture ; si l'appel est forcé,
  « Outil indisponible ». STAFF **avec** `CATALOG_MANAGE` : peut créer une catégorie mais **pas** modifier le slogan
  (réservé ADMIN).
- **Statut** : À faire

## ACT-18 — Journal d'audit
- **P1** · Manuel
- **Attendu** : chaque écriture exécutée apparaît dans `/admin/audit` (action `ASSISTANT_ACTION`, mode « automatique » ou
  « confirmée », outil et arguments) ; l'annulation est journalisée aussi ; les lectures ne le sont pas.
- **Statut** : À faire

## ACT-19 — Suivi d'usage (journaux)
- **P3** · Manuel
- **Attendu** : chaque tour écrit une ligne `assistant_turn store=… tools=… actions=[…] ms=…` **sans contenu de
  conversation**.
- **Statut** : À faire

## ACT-20 — Panne du modèle après des actions
- **P2** · Manuel (simulé)
- **Étapes** : couper le réseau vers Gemini juste après une première action.
- **Attendu** : les actions déjà faites restent affichées avec une phrase neutre (« C'est fait. ») ; sans action, message
  « momentanément indisponible » et le **quota n'est pas consommé**.
- **Statut** : À faire

## ACT-21 — Quota quotidien de messages
- **P1** · Manuel/Auto
- **Préconditions** : `AI_DAILY_LIMIT_BASIC=3`.
- **Attendu** : au 4e message : « Limite quotidienne atteinte (3 messages par jour sur le plan Basic) » (429) ; un message
  en échec côté fournisseur ne consomme pas le quota.
- **Statut** : À faire

## ACT-22 — Un seul bouton d'annulation actif
- **P3** · Manuel
- **Étapes** : enchaîner 2 actions annulables dans le même message.
- **Attendu** : seul le bouton de la **dernière** action est actif ; la première reste annulable via un nouveau message
  ou n'est plus proposée (limite connue).
- **Statut** : À faire

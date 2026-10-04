# 08 — Confirmation d'une commande par WhatsApp

Écran : `/admin/commandes` → détail d'une commande → bouton **« Confirmer par WhatsApp »** (tous les plans : lien
`https://wa.me/<numéro>?text=…`, le commerçant envoie lui-même le message ; aucun compte WhatsApp Business requis).

Message : récapitulatif (articles × quantités, total à payer à la livraison, ville) + demande de répondre « OUI ».
Langue du message : **langue par défaut de la boutique** (fr / en / ar), pas celle de l'interface admin.

---

## WHA-01 — Bouton présent et lien correct
- **P1** · Manuel
- **Préconditions** : commande avec client `Nadia`, téléphone `06 12 34 56 78`, ville Rabat, 2 articles.
- **Attendu** : le bouton ouvre un nouvel onglet vers `https://wa.me/212612345678?text=…` ; le texte (décodé) contient le
  nom du client, le nom de la boutique, les articles, le total et la ville.
- **Statut** : À faire

## WHA-02 — Normalisation des numéros marocains
- **P1** · Auto
- **Attendu** :

  | Saisie | Numéro utilisé |
  |---|---|
  | `06 12 34 56 78` | `212612345678` |
  | `+212 6 12 34 56 78` | `212612345678` |
  | `0612345678` | `212612345678` |
  | `612345678` | `212612345678` |
  | `00212612345678` | `212612345678` |
  | `12345`, `abc`, vide | aucun lien |
- **Statut** : À faire

## WHA-03 — Numéro inutilisable
- **P1** · Manuel
- **Attendu** : à la place du bouton : « Numéro du client inutilisable pour WhatsApp. » ; le reste du détail s'affiche
  normalement.
- **Statut** : À faire

## WHA-04 — Langue du message
- **P2** · Manuel
- **Étapes** : boutique en langue par défaut `ar`, puis `en`, puis sans langue.
- **Attendu** : message en arabe (RTL dans WhatsApp), en anglais, puis en français par défaut ; l'interface admin garde
  sa propre langue.
- **Statut** : À faire

## WHA-05 — Plan Basic
- **P1** · Manuel · `test-basic`
- **Attendu** : le bouton est disponible (la fonction n'est **pas** réservée au plan Pro) ; aucun appel à une API
  WhatsApp Business.
- **Statut** : À faire

## WHA-06 — Commande sans nom de produit
- **P3** · Manuel
- **Attendu** : les lignes sans nom de produit sont omises du message, sans plantage.
- **Statut** : À faire

## WHA-07 — Caractères spéciaux
- **P2** · Manuel
- **Étapes** : nom de client `Hamza & fils`, produit `Sac "Atlas" 50%`.
- **Attendu** : le texte arrive intact dans WhatsApp (encodage d'URL correct, pas de coupure à `&`, `%`, `#`).
- **Statut** : À faire

## WHA-08 — Intégration mobile
- **P3** · Manuel (Android)
- **Attendu** : sur téléphone, le lien ouvre l'application WhatsApp avec le message prêt ; sur bureau, WhatsApp Web.
- **Statut** : À faire

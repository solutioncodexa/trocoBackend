# 07 — Produit sans photo et import CSV

## Produit sans photo (`PSP`)

Règle : une photo n'est **plus obligatoire** à la création. Sans photo, l'API renvoie `/placeholder.svg` (fichier
statique de la vitrine) à la place de la liste d'images ; la base ne contient **aucune** image.

### PSP-01 — Création depuis l'admin sans photo
- **P1** · Manuel
- **Étapes** : `/admin/produits` → Ajouter → nom, prix, catégorie, **aucune image** → Enregistrer.
- **Attendu** : toast d'information « Produit enregistré sans photo… » (non bloquant) ; produit créé ; HTTP 201.
- **Statut** : À faire

### PSP-02 — Création par API sans fichier
- **P1** · Auto
- **Étapes** : `POST /products` multipart avec la partie `product` seulement.
- **Attendu** : 201 (avant : 400 « Au moins une image est requise »).
- **Statut** : À faire

### PSP-03 — Affichage sur la vitrine
- **P1** · Manuel
- **Attendu** : fiche produit, liste, accueil (blocs « produits ») et panier affichent l'**image par défaut**, **sans icône
  d'image cassée** et sans erreur console.
- **Statut** : À faire

### PSP-04 — Édition d'un produit sans photo
- **P1** · Manuel
- **Étapes** : ouvrir le produit en édition, changer le prix, enregistrer sans ajouter d'image.
- **Attendu** : le formulaire **ne montre pas** l'image par défaut comme une vraie image ; l'enregistrement réussit ;
  aucune image « /placeholder.svg » n'est enregistrée en base.
- **Statut** : À faire

### PSP-05 — Ajouter la photo plus tard
- **P1** · Manuel
- **Attendu** : ajouter une image remplace l'image par défaut sur la vitrine.
- **Statut** : À faire

### PSP-06 — Produits d'exemple (packs par secteur)
- **P1** · Manuel
- **Étapes** : assistant de création → choisir « Mode » → ajouter les produits d'exemple.
- **Attendu** : les 6 produits d'exemple sont **créés** (ils échouaient avant) avec leurs catégories ; SKU `DEMO-MODE-0x` ;
  « Supprimer les produits d'exemple » les retire tous.
- **Statut** : À faire

### PSP-07 — Aperçus et partages
- **P2** · Manuel
- **Attendu** : lien de partage / aperçu (réseaux, WhatsApp) d'un produit sans photo : pas d'erreur serveur ; image
  par défaut ou absence propre.
- **Statut** : À faire

### PSP-08 — Autres consommateurs de l'API
- **P2** · Manuel
- **Attendu** : liste des commandes, produits en vedette, favoris et API headless affichent le produit sans plantage.
- **Statut** : À faire

---

## Import CSV (`IMP`)

Page : `/admin/produits/import` (bouton « Importer (CSV) » de la liste des produits). Droit : `PRODUCTS_CREATE`.
Les photos **ne sont pas importées**. 500 lignes maximum.

### Fichiers d'essai

**Modèle Get STORE** (séparateur `;`, bouton « Télécharger le modèle ») :

```csv
nom;prix;categorie;stock;sku;description
T-shirt coton;149;Vêtements;20;TSH-001;T-shirt en coton épais
Sac cabas;549;Accessoires;8;SAC-002;Grand sac en cuir
```

**Export Shopify** (virgule, lignes de variantes sans titre) :

```csv
Handle,Title,Body (HTML),Vendor,Product Category,Variant SKU,Variant Price,Variant Compare At Price,Variant Inventory Qty,Image Src
tshirt,"T-shirt, coton","<p>Doux &amp; léger</p>",Atlas,"Apparel > Shirts",TS1,149.00,199.00,20,http://x/y.jpg
tshirt,,,,,TS2,149.00,,15,http://x/z.jpg
sac,Sac cabas,,Atlas,Bags,SB1,"1 549,50",,3,
x,P,,,,,abc,,,
```

### IMP-01 — Modèle Get STORE
- **P1** · Manuel
- **Attendu** : aperçu de 2 lignes « Prêt » ; « Importer 2 produit(s) » crée 2 produits, les catégories **Vêtements** et
  **Accessoires** (si absentes) ; le message « 2 produit(s) importé(s) ✅ » ; les produits apparaissent dans la liste, avec
  l'image par défaut.
- **Statut** : À faire

### IMP-02 — Export Shopify
- **P1** · Manuel
- **Attendu** : aperçu = **3 lignes** (T-shirt, Sac, ligne `x`) : la ligne de variante `TS2` est ignorée ; « T-shirt,
  coton » garde sa virgule ; description « Doux & léger » (HTML retiré) ; prix barré 199 conservé (supérieur à 149) ;
  catégorie « Shirts » (dernier niveau) ; « 1 549,50 » → 1549,50 ; la ligne `x` est marquée « Prix invalide » et
  **n'est pas importée** ; colonnes ignorées affichées (Vendor, Image Src).
- **Statut** : À faire

### IMP-03 — Colonnes obligatoires absentes
- **P1** · Manuel/Auto
- **Étapes** : fichier dont l'en-tête est `foo;bar`.
- **Attendu** : « Colonne obligatoire absente : Nom, Prix » ; aucun aperçu, aucun import.
- **Statut** : À faire

### IMP-04 — Doublons
- **P1** · Manuel
- **Étapes** : réimporter le même fichier.
- **Attendu** : toutes les lignes « Déjà présent » (nom identique, sans tenir compte de la casse), **0 import** possible ;
  deux lignes de même nom **dans le fichier** : la 2e est « Déjà présent ».
- **Statut** : À faire

### IMP-05 — Formats de prix
- **P2** · Auto
- **Attendu** (vérifié par un essai hors dépôt) : `1 200,50` → 1200,5 ; `199.00 MAD` → 199 ; `1,200.50` → 1200,5 ;
  `12,50` → 12,5 ; `1,200` → 1200 ; `1.234,5` → 1234,5 ; `0`, `abc` et la cellule vide → **refusés** (« Prix
  invalide ») ; les négatifs aussi.
- **Statut** : À faire

### IMP-06 — Séparateurs et encodage
- **P2** · Manuel
- **Attendu** : `,`, `;` et tabulation détectés automatiquement ; fichier avec BOM UTF-8 (Excel) lu sans caractère
  parasite ; champs entre guillemets contenant retours à la ligne et `""` lus correctement.
- **Statut** : À faire

### IMP-07 — Ligne sans catégorie
- **P2** · Manuel
- **Attendu** : la catégorie « Divers » (« Miscellaneous » / « متفرقات » selon la langue) est utilisée/créée.
- **Statut** : À faire

### IMP-08 — Catégorie au nom proche d'une existante
- **P2** · Manuel
- **Préconditions** : catégorie « Vêtements » (slug `vetements`) existe.
- **Attendu** : une ligne « Vetements » **réutilise** la catégorie existante (pas de doublon ni d'erreur de slug).
- **Statut** : À faire

### IMP-09 — Échec partiel
- **P1** · Manuel
- **Étapes** : importer 10 lignes dont 1 provoque une erreur serveur (ex. SKU invalide / limite du plan).
- **Attendu** : les 9 autres sont créées ; le résumé indique « 9 produit(s) importé(s) ✅ » **et** « 1 échec : <nom> —
  <message> » ; l'import n'est pas interrompu.
- **Statut** : À faire

### IMP-10 — Plus de 500 lignes
- **P2** · Manuel
- **Attendu** : message « Seules les 500 premières lignes sont importées » ; 500 produits créés.
- **Statut** : À faire

### IMP-11 — Droits
- **P1** · Manuel
- **Étapes** : STAFF **sans** `PRODUCTS_CREATE` ouvre `/admin/produits/import` ; le bouton « Importer » est-il visible ?
- **Attendu** : page refusée, bouton absent de la liste des produits.
- **Statut** : À faire

### IMP-12 — Limite de produits du plan
- **P2** · Manuel
- **Attendu** : au-delà de `maxProducts`, les lignes concernées échouent avec le message du serveur ; les précédentes
  restent créées.
- **Statut** : À faire

### IMP-13 — Progression et annulation de l'écran
- **P3** · Manuel
- **Attendu** : « Import en cours… n/total » mis à jour ; les boutons sont désactivés pendant l'import ; « Importer un
  autre fichier » remet l'écran à zéro après.
- **Statut** : À faire

### IMP-14 — Textes en trois langues
- **P3** · Manuel
- **Attendu** : tout l'écran est traduit (FR/EN/AR) ; en arabe le tableau et les boutons sont alignés à droite.
- **Statut** : À faire

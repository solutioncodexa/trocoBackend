# 11 — Langues (français, anglais, arabe) et sens de lecture

Règles :
- les pages **inscription** et **admin** partagent la **même langue** (sélecteur dans l'inscription, mémorisée dans le
  navigateur) ;
- l'assistant répond **dans la langue de l'interface**, sauf si la personne écrit clairement dans une autre (darija
  comprise) ;
- l'arabe s'affiche **de droite à gauche** (`dir="rtl"`) ; adresses, emails, téléphones et URL restent **de gauche à
  droite**.

---

## LNG-I01 — Sélecteur de langue à l'inscription
- **P1** · Manuel
- **Étapes** : sur `/creer-boutique`, cliquer « العربية » puis « English » puis « Français ».
- **Attendu** : tous les textes de la page changent (titre, libellés, erreurs, messages de succès, bouton) ; l'arabe passe
  la page en RTL (retour « ← » inversé, bouton « œil » côté gauche) ; la langue reste choisie après rechargement.
- **Statut** : À faire

## LNG-I02 — Continuité inscription → admin
- **P1** · Manuel
- **Étapes** : s'inscrire en arabe.
- **Attendu** : l'assistant de création et le tableau de bord s'ouvrent **en arabe** (RTL).
- **Statut** : À faire

## LNG-I03 — Champs techniques en LTR
- **P2** · Manuel (arabe)
- **Attendu** : email, mot de passe, téléphone et l'URL de la boutique restent lisibles de gauche à droite et alignés au
  début de la ligne ; le curseur ne « saute » pas.
- **Statut** : À faire

## LNG-I04 — Messages d'erreur traduits
- **P2** · Manuel
- **Attendu** : en EN/AR, les erreurs de validation (« Enter a valid email address », « أدخل بريدًا إلكترونيًا صالحًا. »)
  sont dans la langue choisie ; les erreurs **du serveur** restent dans la langue du serveur (limite connue).
- **Statut** : À faire

## LNG-I05 — Secteurs
- **P3** · Manuel
- **Attendu** : les 6 secteurs ont un libellé traduit (EN/AR) ; l'identifiant envoyé reste celui du pack (`mode`, `beaute`…).
- **Statut** : À faire

---

## Assistant (chat et parcours)

### LNG-A01 — Interface du chat
- **P1** · Manuel
- **Attendu** : bouton, titre, boutons des parcours, widgets (Oui/Non, Passer, Appliquer…) traduits ; en arabe, le chat
  s'ouvre en RTL et les chiffres/URL (`/admin/…`) restent lisibles.
- **Statut** : À faire

### LNG-A02 — Réponse libre dans la langue de l'interface
- **P1** · Manuel
- **Étapes** : interface en anglais, poser une question en français ; puis en darija.
- **Attendu** : réponse en français (langue de la question) puis en darija ; interface en arabe + question en français :
  réponse en français ; **les adresses `/admin/…` sont citées telles quelles**.
- **Statut** : À faire

### LNG-A03 — Parcours complets dans chaque langue
- **P1** · Manuel
- **Étapes** : refaire un parcours court (Configuration, puis Contenu) en FR, EN, AR.
- **Attendu** : questions, suggestions, récapitulatifs et messages de fin traduits ; **aucune clé de traduction brute** (`assistant.xxx`)
  visible ; les textes proposés (FAQ, présentation, SEO) sont dans la langue choisie.
- **Statut** : À faire

### LNG-A04 — Contenu généré par langue
- **P1** · Manuel
- **Attendu** : la page FAQ créée en arabe a ses questions/réponses en arabe, en anglais en anglais ; le titre de la page
  Contact est « Contact » / « Contact » / « اتصل بنا » ; les modèles de message WhatsApp sont dans la langue choisie.
- **Statut** : À faire

### LNG-A05 — Pages légales
- **P3** · Manuel
- **Attendu** : les modèles de pages légales sont **en français uniquement** (limite connue) ; le rappel « à relire et à
  faire valider » est affiché.
- **Statut** : À faire

### LNG-A06 — Test de complétude
- **P1** · Auto
- **Attendu** : le test de complétude des traductions de l'admin passe (`fr`, `en`, `ar` ont les mêmes clés), y compris
  `assistant.*`, `import.*`, `launch.*`, `dashboard.setup*`, `orders.confirmWhatsApp*`.
- **Statut** : À faire

---

## Autres écrans

### LNG-O01 — Import CSV et lancement
- **P2** · Manuel
- **Attendu** : `/admin/produits/import` et la bannière de lancement s'affichent en FR/EN/AR ; en arabe les tableaux
  sont alignés à droite.
- **Statut** : À faire

### LNG-O02 — Message WhatsApp de commande
- **P2** · Manuel
- **Attendu** : voir `08-commandes-whatsapp.md` (WHA-04) : langue = langue par défaut de la **boutique**.
- **Statut** : À faire

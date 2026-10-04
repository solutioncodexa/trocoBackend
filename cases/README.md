# Cas de test — Get STORE (troco)

Cas de test **rédigés pour être exécutés à la main** (ou automatisés plus tard) sur les fonctions ajoutées avec
l'assistant de configuration, l'inscription simplifiée et le lancement de boutique.

> Ces cas décrivent le comportement **voulu**. Aucun n'a encore été exécuté : la colonne « Statut » de chaque fichier
> est à remplir au fil des passes de test (`À faire` → `OK` / `KO` + remarque).

## Fichiers

| Fichier | Sujet |
|---|---|
| [01-inscription-creation-boutique.md](01-inscription-creation-boutique.md) | Page `/creer-boutique` : 3 champs, adresse, secteur, 3 langues |
| [02-lancement-boutique-apercu.md](02-lancement-boutique-apercu.md) | Boutique invisible avant lancement, clé d'aperçu, bouton « Lancer » |
| [03-tableau-de-bord-et-demarrage.md](03-tableau-de-bord-et-demarrage.md) | Liste « Premiers pas », boutons « Avec l'assistant », pastille |
| [04-assistant-parcours-guides.md](04-assistant-parcours-guides.md) | Les 10 parcours guidés du chat |
| [05-assistant-actions-ia.md](05-assistant-actions-ia.md) | Outils du modèle : actions, confirmation, annulation, plafonds |
| [06-assistant-redaction-palette.md](06-assistant-redaction-palette.md) | Textes rédigés par le modèle, palette du logo |
| [07-produits-sans-photo-import-csv.md](07-produits-sans-photo-import-csv.md) | Produit sans photo, import CSV |
| [08-commandes-whatsapp.md](08-commandes-whatsapp.md) | Confirmation d'une commande par WhatsApp |
| [09-pwa-admin.md](09-pwa-admin.md) | Application installable (admin) |
| [10-securite-multitenant.md](10-securite-multitenant.md) | Secrets, droits, cloisonnement des boutiques, injection |
| [11-langues-et-rtl.md](11-langues-et-rtl.md) | Français / anglais / arabe, sens de lecture |

## Convention

Chaque cas suit ce modèle :

- **ID** stable (`INS-03`, `LAN-02`, `LNG-A01`…) à citer dans les rapports d'anomalie ;
- **Priorité** : `P1` bloquant pour la mise en production, `P2` important, `P3` confort ;
- **Préconditions**, **Étapes** numérotées, **Résultat attendu** vérifiable ;
- **Type** : `Manuel`, ou `Auto` quand il se prête à un test automatisé (API, logique pure).

## Préparer l'environnement

1. **Backend + front** publiés ensemble (la migration `V48__storefront_live.sql` doit être appliquée).
2. Variables du backend (`.env.troco`) :
   - `AI_ASSISTANT_ENABLED=true`, `AI_API_KEY=…`, `AI_MODEL=gemini-3.5-flash-lite` ;
   - `AI_TOOLS_ENABLED=true` pour les cas de [05-assistant-actions-ia.md](05-assistant-actions-ia.md) (sinon seul le
     chat de conseil est actif).
3. Trois boutiques de test, une par plan :

   | Boutique | Plan | Rôle |
   |---|---|---|
   | `test-basic` | Basic | admin + un compte STAFF **sans** droits catalogue |
   | `test-pro` | Pro | admin |
   | `test-biz` | Business | admin |

4. Un compte **Super Admin** (création de boutique « active tout de suite », changement de plan).
5. Une image de logo de test **colorée** (orange), une **en noir et blanc**, un fichier CSV (modèles dans
   [07-produits-sans-photo-import-csv.md](07-produits-sans-photo-import-csv.md)).
6. Navigateurs : Chrome (bureau) + Chrome Android pour la PWA ; une fenêtre privée pour jouer le « client ».

## Lancer les tests automatisés existants (régression)

```bash
# Front
cd trocoFronend
npx tsc --noEmit -p tsconfig.app.json
npx vitest run                # 91 tests (3 en pause volontaire dans AssistantChat.design.test.tsx)

# Back
cd trocoBackend
./mvnw test -Dtest=PlatformIntegrationTest,AssistantServiceTest,AiCopyServiceTest
```

## Points connus à garder en tête

- Les 3 tests « design » du chat sont en pause (`it.skip`) : les cas `PAR-DES-*` sont leur couverture manuelle.
- Un test vitest signalait une erreur non gérée (`scrollIntoView` absent de jsdom) ; elle est résolue côté page
  d'inscription.
- Le comportement avec le **vrai Gemini** (appels de fonctions, signatures) n'a jamais été vérifié : voir `ACT-01`.

## Volume et préfixes

| Fichier | Préfixes d'ID | Cas |
|---|---|---|
| 01 | `INS` | 15 |
| 02 | `LAN` (lancement) | 13 |
| 03 | `DAS` | 10 |
| 04 | `PAR-BAS`, `PAR-CAT`, `PAR-DES`, `PAR-LIV`, `PAR-PAY`, `PAR-MKT`, `PAR-PRO`, `PAR-CNP`, `PAR-MOD`, `PAR-CNT` | 49 |
| 05 | `ACT` | 22 |
| 06 | `RED`, `PAL` | 13 |
| 07 | `PSP`, `IMP` | 22 |
| 08 | `WHA` | 8 |
| 09 | `PWA` | 10 |
| 10 | `SEC` | 22 |
| 11 | `LNG` | 13 |
| **Total** | | **197** |

## Ordre conseillé pour une première passe

1. `05` **ACT-01** (le plus gros risque : Gemini avec appels de fonctions n'a jamais été essayé).
2. `02` (lancement) et `01` (inscription) : ils changent le parcours de **chaque** nouvelle boutique.
3. `07` **PSP-\*** puis **IMP-\***.
4. `04` parcours guidés, un plan à la fois (Basic d'abord : c'est lui qui a le plus de restrictions).
5. `10` sécurité, puis `11` langues, puis `08`, `09`, `03`, `06`.

## Remplir le suivi

Chaque cas a une ligne `Statut : À faire`. Remplacer par `OK`, ou `KO — <ce qui a été observé>` ; pour une anomalie,
noter l'ID du cas, la boutique de test, le navigateur et l'heure (pour retrouver la ligne de journal du serveur).

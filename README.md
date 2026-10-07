# 🏠 Eterhome

> Plugin de homes pour serveur Minecraft. Document de cadrage développeur pour lancer le développement, pas la doc du plugin final.

> *This README is a living document and may change as the project evolves.*

---

## 🎯 Objectif

Sur n'importe quel serveur, les joueurs peuvent **définir** (`/sethome`), **supprimer** (`/delhome`), **lister** (`/homes`) et **se téléporter** (`/home`) à leurs homes. Avec permission, ils peuvent **voir les homes d'un autre joueur** (`/homes <joueur>`).

Interface : **GUI simple** (inventaire Bukkit, rien de custom).

### 📦 Dépendances (liste fermée)

| Dépendance | Rôle |
|---|---|
| Plugin de permissions (LuckPerms…) | Permissions / limites de homes, lues via l'API Bukkit |
| **EterLib 1.6.0+** (`depend`) | Socle commun : base, Redis, langue et textes communs, joueurs du réseau, téléportation, menus (cadre, Dialogs) (voir le README d'EterLib) |

---

## 🧱 Structure

```
eterhome/
├── core/        # Init uniquement (SQL, cache, messaging…)
├── helper/      # Interfaces génériques + implémentations
├── listeners/   # Déclarations (commands/ et events/)
└── module/      # Logique métier par fonctionnalité
```

> 💡 Les noms de classes sont des **suggestions**. Les 4 dossiers sont fixes, l'organisation interne reste libre.

- **`core`** : ouvre/ferme SQL, cache, messaging, charge la config. Aucune logique métier.
- **`helper`** : consomme `core`, accès technique générique (interface → une classe par implémentation). **Aucune logique métier.**
- **`listeners`** : registre des déclarations de commandes et d'events (`CommandRegistry`, `EventRegistry`). Les fichiers eux-mêmes vivent dans leur module. Indépendant de `helper` et `core`.
- **`module`** : toute la logique métier (`home`, `teleport`, `gui`, `backup`, `permission`).

> ✅ **Tranché** : `core` et `helper` vivent dans **EterLib**, partagé par tous les plugins Eter. EterHome ne contient plus
> que `listeners` et `module` (homes, GUI, permissions) et ne gère **que les homes**.

Dépendances : `module → EterLib (helper, services)`, et `listeners → module`.

---

## ⚡ Cache et communication

**SQL est la source de vérité.** Redis est **optionnel** (réglé dans `EterLib/config.yml`) :

| Mode | Implémentation |
|---|---|
| `cache.enabled: false` | `EterLib#getRedis()` = `null` → le module lit/écrit SQL directement |
| `cache.enabled: true` | `RedisCache`, partagé entre serveurs (copie invalidée à chaque écriture, TTL 1h) |

- auto-complétion des noms de homes seulement avec Redis (sinon SQL à chaque touche)
- Redis n'est jamais requis pour démarrer EterHome

---

## 💾 Backup automatique

```yaml
backup:
  enabled: true
  interval: 6h   # configurable
  keep: 10
  compress: true
```

Tâche **asynchrone**, lit **SQL**, format agnostique du SGBD (ex. JSON), rotation selon `keep`. En cross-server, un seul serveur l'exécute. Commande `/eterhome backup now`. Restauration : en issue.

---

## 🗄️ Données et permissions (propositions)

- `eterhome_homes` : `owner`, `name`, `server`, `world`, `x`, `y`, `z`, `yaw`, `pitch`, `icon` — `PRIMARY KEY (owner, name)`
- Joueurs (`eter_players`), cooldown et téléportations en attente : tables d'EterLib (`eter_*`)
- Permissions joueur (par défaut) : `eterhome.set`, `.delete`, `.rename`, `.teleport`, `.list`
- Permissions admin (op) : `.others.view`, `.others.teleport`, `.others.delete`, `.others.rename`, `.limit.unlimited`, regroupées dans `.admin`
- Dispenses de téléportation : `eter.bypass.warmup`, `.cooldown`, `.combat` (EterLib, valables pour tout le réseau)
- Limite : `eterhome.limit.<n>`, sinon `homes.default-limit` (config). À venir : `.admin.backup`

## 🌍 Messages et téléportation

- Messages en **MiniMessage** dans `lang/<locale>.yml` (codes Minecraft), choisis selon la langue du client, **`en_us` par défaut**. Palette commune (`<primary>`, `<accent>`, `<success>`, `<error>`, `<info>`) définie dans `EterLib/config.yml`.
- GUI `/homes` : cadre orange (rouge en vue admin), icône par dimension ou choisie par le joueur, tête du joueur avec limite et cooldown en direct. Création, modification et suppression via les **Dialogs** natifs (client 1.21.6+), sans saisie dans le chat.
- Téléportation (EterLib, commune à /home, /tpa…) : **combat** (coup donné/reçu, monstres compris) → **cooldown** (partagé entre serveurs) → **warmup** (bossbar, particules, annulé si on bouge ou prend des dégâts) → départ.

---

## 🔀 Workflow Git

- **Phase 1** : la fondation d'un seul bloc, puis **un commit détaillé** (ce qui est posé, choix d'architecture, ce qui est laissé de côté)
- **Phase 2** : tout par **issues** (pas d'avancée sans réflexion) → branche liée `feat/12-home-limit` → commits `feat(home): limite par permission (#12)` → PR `Closes #12`

---

## ❓ Questions ouvertes

1. ~~Téléportation inter-serveurs en v1 ou en issue ?~~ → v1 : téléportation en attente (Redis avec TTL, sinon table `pending_teleports`) + `Connect` via le canal `BungeeCord`, appliquée au spawn (`AsyncPlayerSpawnLocationEvent`)
2. ~~Vault, LuckPerms, ou les deux ?~~ → API de permissions Bukkit (fonctionne avec LuckPerms) ; Vault seulement si on doit lire les permissions d'un joueur hors ligne
3. ~~Helpers SQL : une classe par SGBD ou `SqlDialect` ?~~ → une seule classe `Database` (MySQL/MariaDB)
4. Version cible (Minecraft / Java / Paper) ?
5. Téléportation : délai, cooldown, annulation au mouvement dès la v1 ?
6. Backup : format, restauration, valeurs par défaut ?

---

## 📌 Principes

- `core` initialise, `helper` consomme, `module` décide, `listeners` déclarent
- Aucune I/O bloquante sur le thread principal
- Cache Redis optionnel, SQL toujours source de vérité
- Pas de feature sans issue (après la phase 1)

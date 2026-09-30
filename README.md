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
| Vault **ou** LuckPerms (`softdepend`) | Permissions / limites de homes |
| HikariCP (shadée) | Pool SQL |
| Jedis (shadée, optionnelle à l'exécution) | Client Redis |

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

```
helper/
├── sql/        SqlHelper → MySqlHelper, PostgreSqlHelper
├── cache/      CacheHelper → NoopCacheHelper, LocalCacheHelper, RedisCacheHelper
└── messaging/  MessagingHelper → NoopMessaging, RedisMessaging
```

Dépendances : `module → helper → core`, et `listeners → module`.

> ⚡ **Piste** : une seule implémentation JDBC + une petite interface `SqlDialect` plutôt qu'une classe complète par SGBD. À trancher avant le gros commit.

---

## ⚡ Cache et communication

**SQL est la source de vérité.** Le cache est **optionnel** : sans cache configuré, le plugin **requête directement la DB**.

| Mode | Implémentation |
|---|---|
| Aucun cache | `NoopCacheHelper` → lecture/écriture SQL directes |
| Serveur unique | `LocalCacheHelper` (`ConcurrentHashMap`) |
| Cross-server | `RedisCacheHelper` |

- choix dans `config.yml`, résolu **uniquement dans `core`**
- les modules ne parlent qu'à `CacheHelper` (miss = lecture SQL), ils ne savent pas quel mode est actif
- **TTL** configurable (cache-aside) : absent/expiré → lecture SQL puis remise en cache
- `MessagingHelper` (publish/subscribe) = interface de **communication inter-serveurs** : vide aujourd'hui, Redis pub/sub (ou autre) plus tard
- Redis n'est jamais requis pour démarrer

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

- Table `eterhome_homes` : `id`, `owner_uuid`, `name`, `server`, `world`, `x`, `y`, `z`, `yaw`, `pitch`, `created_at` — `UNIQUE (owner_uuid, name)`
- Permissions : `eterhome.set`, `.delete`, `.teleport`, `.list`, `.others.view`, `.others.teleport`, `.others.delete`, `.limit.<n>`, `.bypass.*`, `.admin`, `.admin.backup`

---

## 🔀 Workflow Git

- **Phase 1** : la fondation d'un seul bloc, puis **un commit détaillé** (ce qui est posé, choix d'architecture, ce qui est laissé de côté)
- **Phase 2** : tout par **issues** (pas d'avancée sans réflexion) → branche liée `feat/12-home-limit` → commits `feat(home): limite par permission (#12)` → PR `Closes #12`

---

## ❓ Questions ouvertes

1. Téléportation inter-serveurs en v1 ou en issue ?
2. Vault, LuckPerms, ou les deux ?
3. Helpers SQL : une classe par SGBD ou `SqlDialect` ?
4. Version cible (Minecraft / Java / Paper) ?
5. Téléportation : délai, cooldown, annulation au mouvement dès la v1 ?
6. Backup : format, restauration, valeurs par défaut ?

---

## 📌 Principes

- `core` initialise, `helper` consomme, `module` décide, `listeners` déclarent
- Aucune I/O bloquante sur le thread principal
- Cache et Redis optionnels, toujours derrière une interface
- Pas de feature sans issue (après la phase 1)

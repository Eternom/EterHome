# Eterhome

> Plugin de homes pour serveur Minecraft. Ce README est un **document de cadrage développeur** : il sert à lancer le développement, pas à documenter le plugin final (la doc utilisateur viendra plus tard).

---

## 1. Objectif

Permettre aux joueurs, sur n'importe quel serveur :

- de **définir** des homes (`/sethome`)
- de **supprimer** leurs homes (`/delhome`)
- de **se téléporter** à leurs homes (`/home`)
- de **lister / récupérer** leurs homes (`/homes`)
- avec la permission adéquate, de **voir (et éventuellement gérer) les homes d'un autre joueur** (`/homes <joueur>`)

Interface : **GUI simple** (inventaire Bukkit standard, aucun GUI custom, pas de resource pack).

### Dépendances

Liste **fermée** : rien d'autre n'est autorisé sans issue validée.

| Dépendance | Type | Rôle |
|---|---|---|
| Vault **ou** LuckPerms | Plugin (`softdepend`) | Permissions / limites de homes |
| **HikariCP** | Lib (shadée) | Pool de connexions SQL (MySQL / PostgreSQL) |
| **Jedis** | Lib (shadée) | Client Redis (cache + pub/sub en cross-server) |

Les libs sont *shadées* et *relocalisées* dans le jar pour éviter les conflits avec d'autres plugins.

---

## 2. Structure du projet

```
eterhome/
├── core/            # Initialisation uniquement (SQL, Redis, cache…)
├── helper/          # Interfaces génériques + implémentations
├── listeners/       # Déclarations uniquement (enregistrement des commandes et events)
│   ├── events/      # Déclare / enregistre les fichiers contenant des events
│   └── commands/    # Déclare / enregistre les fichiers contenant des commandes
└── module/          # Logique métier par fonctionnalité (home, gui, teleport…)
```

### 2.1 `core` : l'init, et rien d'autre

`core` contient **tous les éléments centraux communs à n'importe quel plugin** et se limite à l'**initialisation** :

- ouverture / fermeture du pool SQL
- connexion / fermeture Redis
- création du cache
- chargement de la config

Aucune logique métier ici. `core` expose des connexions prêtes à l'emploi, que `helper` consomme.

```
core/
├── CoreManager            # orchestre start() / stop() dans le bon ordre
├── sql/SqlCore            # init du pool (ex. HikariCP)
├── redis/RedisCore        # init du client Redis
└── cache/CacheCore        # choisit et initialise le cache (Redis ou Map locale) selon la config
```

### 2.2 `helper` : interface générale puis une classe par élément

Les `helper` **consomment** ce que `core` a initialisé (pool SQL, client Redis, cache) et l'exposent via des méthodes techniques génériques (get, set, delete…). **Ils ne contiennent aucune logique métier** : règles, validations, limites, permissions, décisions de cache (TTL, invalidation) vivent dans les `module`.

Derrière `core`, on passe par un **helper** qui suit un schéma fixe :

1. une **interface générale** qui décrit les méthodes (get, set, delete…)
2. une **classe par implémentation** qui reprend l'interface

Exemple pour le SQL :

```
helper/
├── sql/
│   ├── SqlHelper            # interface : get / set / delete / exists…
│   ├── MySqlHelper          # implémente SqlHelper
│   └── PostgreSqlHelper     # implémente SqlHelper
├── redis/
│   ├── RedisHelper          # interface
│   └── RedisHelperImpl
└── cache/
    ├── CacheHelper          # interface (get / set / delete / exists / TTL…)
    ├── RedisCacheHelper     # mode cross-server : Redis
    └── LocalCacheHelper     # mode serveur unique : Map gérée par le plugin
```

> **Piste d'optimisation à trancher avant de coder**
> Une classe complète par SGBD duplique beaucoup de code (les requêtes sont quasi identiques). Alternative plus légère :
> - **une seule implémentation** `SqlHelperImpl` basée sur JDBC
> - les différences (syntaxe d'upsert, types, auto-increment) isolées dans une petite interface `SqlDialect` (`MySqlDialect`, `PostgreSqlDialect`)
>
> On garde l'interface générale (testabilité, remplacement facile) mais on évite de dupliquer toute la couche par SGBD. **À décider en issue #0 / avant le gros commit.**

### 2.3 `listeners` : le registre des déclarations

`listeners` est **séparé de `helper`** et ne contient **ni logique métier, ni accès données**. On y stocke uniquement **tous les appels de déclaration** (enregistrement) des fichiers qui comportent des commandes et des events :

- `commands/` : déclare les fichiers de commandes (enregistrement auprès du serveur, nom, alias, exécuteur)
- `events/` : déclare les fichiers d'events (`registerEvents` des listeners Bukkit : join, quit, teleport…)

Les fichiers de commandes et d'events eux-mêmes (le code qui réagit) vivent **dans leur `module`**, à côté du service qu'ils utilisent. `listeners` est le point d'entrée unique qui les branche au plugin : on sait où tout est déclaré en ouvrant un seul endroit.

```
listeners/
├── commands/
│   └── CommandRegistry      # appelle l'enregistrement de chaque commande des modules
└── events/
    └── EventRegistry        # appelle l'enregistrement de chaque listener des modules
```

Règles :

- `listeners` **ne dépend pas de `helper`** ni de `core`, seulement des fichiers exposés par les `module`
- ajouter une commande ou un event = créer le fichier dans le module + **ajouter une ligne** de déclaration dans le registre correspondant

### 2.4 `module`

Une fonctionnalité = un module autonome.

```
module/
├── home/
│   ├── Home               # modèle
│   ├── HomeRepository     # accès données (via helper)
│   ├── HomeService        # logique métier (limites, validation, noms)
│   ├── HomeCommand        # commandes du module (déclarées dans listeners/commands)
│   └── HomeListener       # events du module (déclarés dans listeners/events)
├── teleport/
│   └── TeleportService    # délai, annulation si mouvement, cooldown
├── gui/
│   └── HomeGui            # inventaire simple (liste, clic = tp, clic droit = suppr.)
├── backup/
│   ├── BackupService      # export / rotation des sauvegardes
│   └── BackupScheduler    # planification asynchrone (intervalle configurable)
└── permission/
    └── PermissionService  # abstraction Vault / LuckPerms
```

Sens des dépendances :

- `module → helper → core` (jamais l'inverse)
- `listeners → module` : il ne fait que **déclarer** ce que les modules exposent
- `listeners` est **indépendant de `helper`** et de `core`

---

## 3. Gestion des données en cours : cache obligatoire

Même si le sujet a déjà été discuté en amont, il est **demandé explicitement** : les données en cours (homes des joueurs connectés, etc.) sont **toujours gérées via un service de cache**, jamais par des lectures SQL directes à chaque action.

Le type de cache dépend du mode de déploiement :

| Mode | Cache utilisé | Implémentation |
|---|---|---|
| **Cross-server** (réseau de serveurs) | **Redis** | `RedisCacheHelper` |
| **Serveur unique** | **Cache interne du plugin** (`Map`, de préférence `ConcurrentHashMap`) | `LocalCacheHelper` |

Règles :

- Les deux implémentations respectent la **même interface `CacheHelper`** : les `module` ne savent pas quel cache est utilisé.
- Le mode est choisi **dans `config.yml`** (ex. `cross-server: true|false`) et résolu **uniquement dans `core`** (`CacheCore`).
- **SQL reste la source de vérité** (persistance). Le cache contient les données en cours.
- Flux type : chargement SQL → cache à la connexion, lecture/écriture via le cache, **écriture SQL asynchrone** à chaque modification (write-through), déchargement du cache à la déconnexion (en local) ou expiration par TTL (Redis).
- En **cross-server**, Redis sert aussi à **synchroniser** les serveurs (invalidation / mise à jour via pub/sub) pour qu'un home créé sur un serveur soit visible immédiatement sur les autres.
- En serveur unique, aucune dépendance Redis ne doit être requise pour démarrer le plugin.

#### Astuce : TTL pour récupérer les données de homes

Chaque entrée du cache porte un **TTL** (configurable). Quand l'entrée expire ou est absente, le service **recharge les homes depuis SQL** (pattern *cache-aside*) :

1. lecture dans le cache
2. si présent → on retourne (et on peut rafraîchir le TTL à chaque accès)
3. si absent / expiré → lecture SQL, remise en cache avec TTL, retour

Conséquences :

- pas besoin de gérer finement le déchargement : une entrée inutilisée disparaît seule
- le cache se répare tout seul après un flush Redis, un redémarrage ou une désynchronisation
- `LocalCacheHelper` doit **émuler le TTL** (timestamp d'expiration par entrée + nettoyage périodique) pour garder le même comportement que Redis (`SET key value EX ttl`)
- TTL configurable, ex. `cache.ttl: 10m`


---

## 4. Backup automatique

Le plugin doit prévoir un **système de sauvegarde automatique des homes**, exécuté **tous les X temps, configurable**.

```yaml
backup:
  enabled: true
  interval: 6h        # fréquence (ex. 30m, 6h, 1d)
  keep: 10            # nombre de backups conservés (rotation)
  directory: backups  # dans le dossier du plugin
  compress: true
```

Règles :

- tâche **asynchrone** planifiée, jamais sur le thread principal
- le backup lit **SQL (source de vérité)**, pas le cache, et passe par la couche `helper` : format **agnostique du SGBD** (ex. JSON, optionnellement compressé), donc pas de dépendance à `mysqldump` / `pg_dump`
- fichiers horodatés (`homes-2026-09-30_12-00.json.gz`) avec **rotation** selon `keep`
- en **cross-server**, un seul serveur doit exécuter le backup : verrou Redis (`SET key value NX EX`) pour éviter les doublons
- erreurs loguées sans jamais faire planter le plugin
- prévoir une commande admin (ex. `/eterhome backup now`) et, à terme, une restauration (à traiter en issue)

Module : `module/backup/` (`BackupService`, `BackupScheduler`).

---

## 5. Modèle de données (proposition)

Table `eterhome_homes`

| Colonne | Type | Note |
|---|---|---|
| `id` | BIGINT PK | auto-incrément |
| `owner_uuid` | UUID | propriétaire |
| `name` | VARCHAR(32) | unique par propriétaire |
| `server` | VARCHAR(64) | identifiant serveur (voir questions ouvertes) |
| `world` | VARCHAR(64) | |
| `x`, `y`, `z` | DOUBLE | |
| `yaw`, `pitch` | FLOAT | |
| `created_at` | TIMESTAMP | |

Contrainte : `UNIQUE (owner_uuid, name)`.

---

## 6. Commandes et permissions (proposition)

| Commande | Permission | Description |
|---|---|---|
| `/sethome <nom>` | `eterhome.set` | Crée / met à jour un home |
| `/delhome <nom>` | `eterhome.delete` | Supprime un home |
| `/home <nom>` | `eterhome.teleport` | Téléportation |
| `/homes` | `eterhome.list` | Ouvre la GUI de ses homes |
| `/homes <joueur>` | `eterhome.others.view` | Voir les homes d'un autre joueur |
| (clic GUI sur home d'autrui) | `eterhome.others.teleport` | Se tp chez un autre joueur |
| (suppression chez autrui) | `eterhome.others.delete` | Supprimer le home d'un autre joueur |
| | `eterhome.limit.<n>` | Nombre max de homes |
| | `eterhome.bypass.*` | Bypass cooldown / délai |
| `/eterhome backup now` | `eterhome.admin.backup` | Lance un backup manuel |
| | `eterhome.admin` | Reload, etc. |

---

## 7. Workflow Git

### Phase 1 : la fondation (un gros bloc)

Pas de commit par étape. On avance sur **toute la base d'un coup** (structure, core, helpers, modèle, commandes de base, GUI minimale), puis **un commit détaillé** qui explique :

- ce qui a été posé (arborescence, responsabilités de chaque couche)
- les choix d'architecture et pourquoi
- ce qui est volontairement laissé de côté

### Phase 2 : tout passe par des issues

Après le commit initial : **aucune avancée sans réflexion préalable**.

1. On ouvre une **issue** (contexte, objectif, critères d'acceptation, pistes)
2. On réfléchit / discute dans l'issue
3. On code sur une branche liée : `feat/12-home-limit`, `fix/18-tp-cancel`
4. Commits **liés à l'issue** : `feat(home): limite par permission (#12)`
5. Merge via PR qui ferme l'issue (`Closes #12`)

Convention de commit : [Conventional Commits](https://www.conventionalcommits.org/) (`feat`, `fix`, `refactor`, `docs`, `chore`).

---

## 8. Checklist du commit initial

- [ ] Projet Maven/Gradle, `plugin.yml`, Vault/LuckPerms en `softdepend`, HikariCP + Jedis shadés et relocalisés
- [ ] `CoreManager` + init SQL / Redis / cache
- [ ] Interfaces `helper` + implémentations retenues
- [ ] `CacheHelper` avec `RedisCacheHelper` (cross-server) et `LocalCacheHelper` (Map), sélection via config
- [ ] Flux cache : chargement à la connexion, write-through SQL async, déchargement à la déconnexion
- [ ] Synchronisation Redis pub/sub en mode cross-server
- [ ] TTL configurable sur le cache (cache-aside, TTL émulé côté `LocalCacheHelper`)
- [ ] `BackupService` + `BackupScheduler` (intervalle, rotation, verrou Redis en cross-server)
- [ ] Création automatique de la table au démarrage
- [ ] Modèle `Home`, `HomeRepository`, `HomeService`
- [ ] `PermissionService` (Vault / LuckPerms)
- [ ] Commandes `sethome`, `delhome`, `home`, `homes`
- [ ] GUI simple (liste + clic = tp)
- [ ] `config.yml` (connexions, limites par défaut, délai de tp)
- [ ] Gestion propre du `onDisable` (fermeture des connexions)

---

## 9. Questions ouvertes (à trancher avant de coder)

1. **« N'importe quel serveur »** : les deux modes (serveur unique et cross-server) sont supportés via la config. Reste à préciser si la **téléportation inter-serveurs** fait partie de la v1 ou d'une issue ultérieure (impacte la colonne `server`).
2. **Vault ou LuckPerms** : les deux supportés, ou un seul ? LuckPerms donne accès aux meta (limite de homes) ; Vault reste plus générique.
3. **Helpers SQL** : une classe par SGBD, ou une implémentation unique + `SqlDialect` ?
4. **Synchronisation Redis** : le cache est acté (Redis en cross-server, Map sinon). Reste à définir le détail de la sync entre serveurs (canaux pub/sub, stratégie d'invalidation) et la valeur par défaut du TTL (rafraîchi à chaque accès ou fixe ?).
5. **Version cible** : version de Minecraft / Java / API (Paper ?).
6. **Téléportation** : délai, annulation au mouvement, cooldown dès la v1 ou en issue ?
7. **Backup** : format retenu (JSON ?), restauration en v1 ou en issue, valeurs par défaut de `interval` / `keep`.
8. **Async** : toutes les I/O hors du main thread (recommandé), avec retour sur le thread principal pour la tp.

---

## 10. Principes

- `core` initialise, `helper` consomme `core` et donne un accès technique générique (aucune logique métier), `module` porte toute la logique métier, `listeners` déclarent (commandes et events), sans logique ni accès données.
- Aucune I/O bloquante sur le thread principal.
- Les données en cours passent toujours par le cache (Redis en cross-server, Map sinon) ; SQL = persistance.
- Une responsabilité par classe, une interface dès qu'une implémentation peut changer.
- Dépendances autorisées : Vault / LuckPerms (plugins), HikariCP et Jedis (libs). Rien d'autre.
- Le cache a un TTL : toute donnée absente se recharge depuis SQL.
- Les données sont sauvegardées automatiquement à intervalle configurable.
- Pas de feature sans issue (après la phase 1).

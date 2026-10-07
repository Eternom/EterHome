# EterHome

Les homes du réseau : `/sethome`, `/delhome`, `/home` (même vers un autre serveur) et le menu `/homes`, avec les homes
d'un autre joueur pour le staff. Document développeur, à tenir à jour avec le code.

## Prérequis

- **EterLib 1.6.0+** (`depend`) : base, Redis (facultatif), langues et textes communs, menus (cadre, Dialogs, bouton
  Retour), téléportation commune (y compris vers un autre serveur).
- Permissions lues par l'API Bukkit (LuckPerms ou autre).

## Fonctionnement

- **Homes** (`module/home/HomeManager`, table `eterhome_homes`, clé `owner` + `name`) : serveur, monde, position,
  orientation et icône. **SQL est la source de vérité** ; avec Redis, une copie par joueur (`homes:<uuid>`, 1 h) est
  effacée à chaque modification et relue depuis la base. Appels en tâche de fond.
- **Limite** : `homes.default-limit` (3), augmentée par `eterhome.limit.<n>`, supprimée par `eterhome.limit.unlimited`.
  Remplacer un home existant est toujours permis.
- **Téléportation** : par la téléportation commune d'EterLib (combat, cooldown partagé, attente avec bossbar, puis
  départ), y compris vers un home d'un autre serveur.
- **Menu `/homes`** (`module/gui`) : cadre orange (rouge en vue admin), tête du joueur avec limite et cooldown en direct,
  icône par dimension ou choisie par le joueur. Créer, modifier, supprimer : Dialogs natifs (client 1.21.6+), jamais
  de saisie dans le chat. Bouton du bas : `menus.homes.back-command` (vide = fermer).
- **Auto-complétion** des noms de homes seulement avec Redis (sinon une lecture en base à chaque touche).

## Commandes et permissions

| Commande | Permission | Rôle |
|---|---|---|
| `/sethome [nom]` | `eterhome.set` | Définit un home ici (sans nom : « home ») |
| `/delhome <nom>` | `eterhome.delete` | Supprime un home |
| `/home [nom]` | `eterhome.teleport` | Téléporte à un home (sans nom : « home »), même sur un autre serveur |
| `/homes [joueur]` | `eterhome.list` | Menu des homes (d'un autre joueur : `eterhome.others.view`) |

Joueurs (par défaut) : `eterhome.set`, `.delete`, `.rename`, `.teleport`, `.list`. Staff (op), regroupées dans
`eterhome.admin` : `.others.view`, `.others.teleport`, `.others.delete`, `.others.rename`, `.limit.unlimited`.
Dispenses de téléportation : `eter.bypass.*` d'EterLib, valables pour tout le réseau.

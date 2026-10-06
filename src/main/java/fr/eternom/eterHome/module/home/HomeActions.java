package fr.eternom.eterHome.module.home;

import fr.eternom.eterHome.module.permission.HomePermissions;
import fr.eternom.eterLib.helper.message.Messages;
import fr.eternom.eterLib.module.teleport.Destination;
import fr.eternom.eterLib.module.teleport.TeleportService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Actions sur les homes, partagées par les commandes et le GUI : vérifications, messages au joueur,
 * et aller-retour entre thread principal (Bukkit) et async (base de données).
 * Toutes les méthodes s'appellent sur le thread principal ; then (peut être null) y est rappelé à la fin.
 */
public class HomeActions {

    /** Message à envoyer une fois revenu sur le thread principal : réussite ou non, clé de langue + variables. */
    private record Reply(boolean success, String key, String... placeholders) {
    }

    private final JavaPlugin plugin;
    private final HomeManager homeManager;
    private final TeleportService teleports;
    private final HomePermissions permissions;
    private final Messages messages;
    private final String serverName;

    public HomeActions(JavaPlugin plugin, HomeManager homeManager, TeleportService teleports, HomePermissions permissions,
                       Messages messages, String serverName) {
        this.plugin = plugin;
        this.homeManager = homeManager;
        this.teleports = teleports;
        this.permissions = permissions;
        this.messages = messages;
        this.serverName = serverName;
    }

    /** Crée ou déplace un home du joueur à location. icon = null : icône automatique (ou celle déjà choisie). */
    public void setHome(Player player, String name, Location location, Material icon, Runnable then) {
        if (!Home.isValidName(name)) {
            reply(player, new Reply(false, "home.name-invalid"));
            run(then);
            return;
        }
        int limit = permissions.limit(player);
        Home home = Home.of(player.getUniqueId(), serverName, name, location, icon);
        async(player, () -> switch (homeManager.set(home, limit)) {
            case CREATED -> new Reply(true, "home.set", "home", home.getName());
            case REPLACED -> new Reply(true, "home.moved", "home", home.getName());
            case LIMIT_REACHED -> new Reply(false, "home.limit-reached", "limit", String.valueOf(limit));
        }, reply -> {
            reply(player, reply);
            run(then);
        });
    }

    public void deleteHome(Player actor, UUID owner, String name, Runnable then) {
        async(actor, () -> homeManager.delete(owner, name), deleted -> {
            reply(actor, new Reply(deleted, deleted ? "home.deleted" : "home.not-found", "home", Home.normalize(name)));
            run(then);
        });
    }

    /** Renomme le home (si newName diffère) et change son icône (si icon n'est pas null). */
    public void editHome(Player actor, UUID owner, String oldName, String newName, Material icon, Runnable then) {
        if (!Home.isValidName(newName)) {
            reply(actor, new Reply(false, "home.name-invalid"));
            run(then);
            return;
        }
        String from = Home.normalize(oldName);
        String to = Home.normalize(newName);
        async(actor, () -> {
            if (!from.equals(to)) {
                switch (homeManager.rename(owner, from, to)) {
                    case NOT_FOUND -> {
                        return new Reply(false, "home.not-found", "home", from);
                    }
                    case NAME_TAKEN -> {
                        return new Reply(false, "home.name-taken", "home", to);
                    }
                    case RENAMED -> {
                    }
                }
            }
            if (icon != null && !homeManager.setIcon(owner, to, icon.name())) {
                return new Reply(false, "home.not-found", "home", to);
            }
            return new Reply(true, "home.updated", "home", to);
        }, reply -> {
            reply(actor, reply);
            run(then);
        });
    }

    public void teleportHome(Player actor, UUID owner, String name) {
        async(actor, () -> homeManager.get(owner, name), home -> home.ifPresentOrElse(
                found -> teleport(actor, found),
                () -> reply(actor, new Reply(false, "home.not-found", "home", Home.normalize(name)))));
    }

    /** Les règles communes (combat, cooldown, attente) et le changement de serveur sont gérés par EterLib. */
    public void teleport(Player player, Home home) {
        teleports.teleport(player, Destination.at(home.getServer(), home.getWorld(), home.getX(), home.getY(), home.getZ(),
                home.getYaw(), home.getPitch(), home.getName()));
    }

    /** Exécute task hors du thread principal, puis then sur le thread principal si le joueur est toujours là. */
    public <T> void async(Player player, Supplier<T> task, Consumer<T> then) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            T result;
            try {
                result = task.get();
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.SEVERE, "Erreur pendant une action sur les homes", e);
                Bukkit.getScheduler().runTask(plugin, () -> reply(player, new Reply(false, "error.generic")));
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    then.accept(result);
                }
            });
        });
    }

    private void reply(Player player, Reply reply) {
        messages.send(player, reply.key(), reply.placeholders());
        // Petit retour sonore : clair si ça a marché, grave sinon
        player.playSound(player.getLocation(), reply.success() ? Sound.ENTITY_EXPERIENCE_ORB_PICKUP : Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
    }

    private static void run(Runnable then) {
        if (then != null) {
            then.run();
        }
    }
}

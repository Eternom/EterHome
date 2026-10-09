package fr.eternom.eterHome.module.home;

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import fr.eternom.eterHome.module.permission.HomePermissions;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Locale;
import java.util.Map;

/**
 * Auto-complétion des noms de homes pour /home, /delhome et /sethome.
 * Lue dans la copie Redis (rapide, hors du thread principal), jamais en base à chaque touche.
 */
public class HomeTabListener implements Listener {

    /** Commande -> permission requise pour proposer les homes. */
    private static final Map<String, String> COMMANDS = Map.of(
            "home", HomePermissions.TELEPORT,
            "delhome", HomePermissions.DELETE,
            "sethome", HomePermissions.SET);

    private final HomeManager homeManager;

    public HomeTabListener(HomeManager homeManager) {
        this.homeManager = homeManager;
    }

    @EventHandler
    public void onTabComplete(AsyncTabCompleteEvent event) {
        if (!event.isCommand() || !(event.getSender() instanceof Player player)) {
            return;
        }

        // Buffer : "/home ba" ; seul le premier argument est complété
        String[] parts = event.getBuffer().replaceFirst("^/", "").split(" ", -1);
        if (parts.length != 2) {
            return;
        }
        String command = parts[0].toLowerCase(Locale.ROOT).replaceFirst("^eterhome:", "");
        String permission = COMMANDS.get(command);
        if (permission == null || !player.hasPermission(permission)) {
            return;
        }

        String prefix = Home.normalize(parts[1]);
        event.setCompletions(homeManager.getAll(player.getUniqueId()).stream()
                .map(Home::getName)
                .filter(name -> name.startsWith(prefix))
                .toList());
        // Même sans résultat : ne pas laisser Bukkit proposer les noms de joueurs
        event.setHandled(true);
    }
}

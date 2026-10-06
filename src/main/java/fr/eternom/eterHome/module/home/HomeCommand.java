package fr.eternom.eterHome.module.home;

import fr.eternom.eterLib.helper.message.Messages;
import fr.eternom.eterHome.module.gui.HomeGui;
import fr.eternom.eterHome.module.permission.HomePermissions;
import fr.eternom.eterLib.module.player.PlayerDirectory;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * /sethome [nom], /delhome <nom>, /home [nom] : raccourcis rapides.
 * /homes [joueur] : le GUI, pour ses homes ou ceux d'un autre joueur (admin).
 */
public class HomeCommand implements TabExecutor {

    private static final String DEFAULT_HOME = "home";

    private final HomeActions actions;
    private final HomeGui gui;
    private final PlayerDirectory directory;
    private final Messages messages;

    public HomeCommand(HomeActions actions, HomeGui gui, PlayerDirectory directory, Messages messages) {
        this.actions = actions;
        this.gui = gui;
        this.directory = directory;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "command.players-only");
            return true;
        }
        String name = args.length > 0 ? args[0] : DEFAULT_HOME;

        switch (command.getName()) {
            case "sethome" -> actions.setHome(player, name, player.getLocation(), null, null);
            case "home" -> actions.teleportHome(player, player.getUniqueId(), name);
            case "delhome" -> {
                // Pas de nom par défaut pour une suppression : trop facile d'effacer "home" par erreur
                if (args.length == 0) {
                    messages.send(player, "home.delhome-usage");
                } else {
                    actions.deleteHome(player, player.getUniqueId(), name, null);
                }
            }
            case "homes" -> openHomes(player, args);
            default -> {
                return false;
            }
        }
        return true;
    }

    /** Joueurs en ligne pour /homes <joueur>. Les noms de homes sont proposés par {@link HomeTabListener}. */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equals("homes") || args.length != 1 || !sender.hasPermission(HomePermissions.OTHERS_VIEW)) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(online -> online.toLowerCase(Locale.ROOT).startsWith(prefix))
                .toList();
    }

    private void openHomes(Player player, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase(player.getName())) {
            gui.open(player, player.getUniqueId(), player.getName());
            return;
        }
        if (!player.hasPermission(HomePermissions.OTHERS_VIEW)) {
            messages.send(player, "player.no-permission-others");
            return;
        }
        // Cherché en base : le joueur ciblé n'est peut-être jamais venu sur ce serveur
        actions.async(player, () -> directory.find(args[0]), target -> target.ifPresentOrElse(
                found -> gui.open(player, found.uuid(), found.name()),
                () -> messages.send(player, "player.unknown", "player", args[0])));
    }
}

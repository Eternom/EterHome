package fr.eternom.eterHome.module.gui;

import fr.eternom.eterHome.module.home.Home;
import fr.eternom.eterHome.module.home.HomeActions;
import fr.eternom.eterHome.module.home.HomeManager;
import fr.eternom.eterHome.module.permission.HomePermissions;
import fr.eternom.eterLib.helper.gui.BackButton;
import fr.eternom.eterLib.helper.message.Messages;
import fr.eternom.eterLib.module.teleport.TeleportService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;

/**
 * Ouvre le menu des homes et branche ses boutons sur les actions et les fenêtres de dialogue.
 * Même menu pour un joueur (ses homes) et un admin (les homes d'un autre) : seuls les droits et le cadre changent.
 * Toutes les méthodes s'appellent sur le thread principal.
 */
public class HomeGui {

    private record MenuData(List<Home> homes, long cooldownSeconds) {
    }

    private final JavaPlugin plugin;
    private final HomeActions actions;
    private final HomeManager homeManager;
    private final HomePermissions permissions;
    private final HomeDialogs dialogs;
    private final TeleportService teleports;
    private final Messages messages;
    private final String serverName;
    private final BackButton backButton;

    public HomeGui(JavaPlugin plugin, HomeActions actions, HomeManager homeManager, HomePermissions permissions,
                   HomeDialogs dialogs, TeleportService teleports, Messages messages, String serverName, BackButton backButton) {
        this.plugin = plugin;
        this.actions = actions;
        this.homeManager = homeManager;
        this.permissions = permissions;
        this.dialogs = dialogs;
        this.teleports = teleports;
        this.messages = messages;
        this.serverName = serverName;
        this.backButton = backButton;
    }

    public void open(Player viewer, UUID owner, String ownerName) {
        int limit = permissions.limit(viewer);
        boolean own = viewer.getUniqueId().equals(owner);
        actions.async(viewer,
                () -> new MenuData(homeManager.getAll(owner), own ? teleports.cooldownSeconds(viewer) : 0),
                data -> viewer.openInventory(
                        new HomeMenu(this, viewer, owner, ownerName, data.homes(), limit, data.cooldownSeconds()).getInventory()));
    }

    void teleport(Player viewer, Home home) {
        viewer.closeInventory();
        actions.teleport(viewer, home);
    }

    /** Position prise au clic ; nom proposé : le premier libre (home1, home2...). */
    void create(Player viewer) {
        Location location = viewer.getLocation();
        Runnable reopen = () -> open(viewer, viewer.getUniqueId(), viewer.getName());
        viewer.closeInventory();
        actions.async(viewer, () -> homeManager.nextFreeName(viewer.getUniqueId()), suggested -> dialogs.create(viewer, suggested,
                form -> actions.setHome(viewer, form.name(), location, form.icon(), reopen),
                reopen));
    }

    void edit(Player viewer, Home home, String ownerName) {
        Runnable reopen = () -> open(viewer, home.getOwner(), ownerName);
        viewer.closeInventory();
        dialogs.edit(viewer, home,
                form -> actions.editHome(viewer, home.getOwner(), home.getName(), form.name(), form.icon(), reopen),
                reopen);
    }

    void confirmDelete(Player viewer, Home home, String ownerName) {
        Runnable reopen = () -> open(viewer, home.getOwner(), ownerName);
        viewer.closeInventory();
        dialogs.confirmDelete(viewer, home, home.getDisplayIcon(serverName),
                () -> actions.deleteHome(viewer, home.getOwner(), home.getName(), reopen),
                reopen);
    }

    /** Secondes de combat restantes (0 si le joueur en est dispensé). */
    long combatSeconds(Player player) {
        return teleports.combatSeconds(player);
    }

    JavaPlugin plugin() {
        return plugin;
    }

    HomePermissions permissions() {
        return permissions;
    }

    Messages messages() {
        return messages;
    }

    String serverName() {
        return serverName;
    }

    BackButton backButton() {
        return backButton;
    }
}

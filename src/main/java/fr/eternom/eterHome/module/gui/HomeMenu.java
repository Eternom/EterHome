package fr.eternom.eterHome.module.gui;

import fr.eternom.eterHome.module.home.Home;
import fr.eternom.eterHome.module.permission.HomePermissions;
import fr.eternom.eterLib.helper.gui.Items;
import fr.eternom.eterLib.helper.gui.Menu;
import fr.eternom.eterLib.helper.gui.Sounds;
import fr.eternom.eterLib.helper.message.Messages;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Menu des homes, 6 lignes :
 * <pre>
 *  ▣ ▣ ▢ ▢ ☺ ▢ ▢ ▣ ▣     ☺ = joueur (homes, limite, cooldown en direct)
 *  ▣ · · · · · · · ▣     · = homes, 28 par page
 *  ▢ · · · · · · · ▢     ▣ = cadre orange (rouge en mode admin), ▢ = cadre gris
 *  ▢ · · · · · · · ▢
 *  ▣ · · · · · · · ▣
 *  ◀ ▣ ▢ ▢ + ▢ ▢ ▣ ▶     + = nouveau home, ◀ ▶ = pages
 * </pre>
 * Clic gauche = se téléporter, Maj + clic gauche = modifier, clic droit = supprimer (selon les droits du viewer).
 */
public class HomeMenu implements Menu {

    private static final int INFO = 4;
    private static final int PREVIOUS = 45;
    private static final int CREATE = 49;
    private static final int NEXT = 53;
    private static final int EMPTY = 22;
    private static final Set<Integer> ACCENT_FRAME = Set.of(0, 1, 7, 8, 9, 17, 36, 44, 46, 52);
    private static final List<Integer> HOME_SLOTS = homeSlots();
    private static final int GAUGE_SIZE = 10;

    private final HomeGui gui;
    private final Messages messages;
    private final Player viewer;
    private final UUID owner;
    private final String ownerName;
    private final List<Home> homes;
    private final int limit;
    private final long cooldownUntil;
    private final Inventory inventory;
    private final Map<Integer, Home> homeAtSlot = new HashMap<>();
    private BukkitTask refresher;
    private int page;

    /** @param cooldownSeconds cooldown restant à l'ouverture (propres homes uniquement), décompté ensuite en direct */
    HomeMenu(HomeGui gui, Player viewer, UUID owner, String ownerName, List<Home> homes, int limit, long cooldownSeconds) {
        this.gui = gui;
        this.messages = gui.messages();
        this.viewer = viewer;
        this.owner = owner;
        this.ownerName = ownerName;
        this.homes = homes;
        this.limit = limit;
        this.cooldownUntil = System.currentTimeMillis() + cooldownSeconds * 1000;
        this.inventory = Bukkit.createInventory(this, 54, isOwn()
                ? text("gui.title-own")
                : text("gui.title-other", "player", ownerName));
        render();
        if (isOwn()) {
            // Cooldown et combat changent chaque seconde : seul l'item du joueur est redessiné
            refresher = Bukkit.getScheduler().runTaskTimer(gui.plugin(), this::refreshInfo, 20, 20);
        }
    }

    @Override
    public void onClick(Player player, int slot, ClickType click) {
        Home home = homeAtSlot.get(slot);
        if (home != null) {
            HomePermissions permissions = gui.permissions();
            if (click == ClickType.SHIFT_LEFT && permissions.canRename(player, owner)) {
                Sounds.click(player);
                gui.edit(player, home, ownerName);
            } else if (click == ClickType.LEFT && permissions.canTeleport(player, owner)) {
                Sounds.click(player);
                gui.teleport(player, home);
            } else if (click.isRightClick() && permissions.canDelete(player, owner)) {
                Sounds.click(player);
                gui.confirmDelete(player, home, ownerName);
            }
        } else if (slot == PREVIOUS && page > 0) {
            page--;
            Sounds.page(player);
            render();
        } else if (slot == NEXT && hasNextPage()) {
            page++;
            Sounds.page(player);
            render();
        } else if ((slot == CREATE || slot == EMPTY && homes.isEmpty()) && canCreate()) {
            Sounds.click(player);
            gui.create(player);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private void render() {
        inventory.clear();
        homeAtSlot.clear();
        drawFrame();

        int start = page * HOME_SLOTS.size();
        for (int i = 0; i < HOME_SLOTS.size() && start + i < homes.size(); i++) {
            Home home = homes.get(start + i);
            homeAtSlot.put(HOME_SLOTS.get(i), home);
            inventory.setItem(HOME_SLOTS.get(i), homeItem(home));
        }
        if (homes.isEmpty()) {
            inventory.setItem(EMPTY, isOwn()
                    ? Items.item(Material.COMPASS, text("gui.empty-own"), canCreate() ? List.of(text("gui.empty-own-lore")) : List.of())
                    : Items.item(Material.COMPASS, text("gui.empty-other", "player", ownerName), List.of()));
        }

        if (page > 0) {
            inventory.setItem(PREVIOUS, Items.item(Material.ARROW, text("gui.previous"),
                    List.of(text("gui.page", "page", String.valueOf(page), "pages", String.valueOf(pageCount())))));
        }
        if (hasNextPage()) {
            inventory.setItem(NEXT, Items.item(Material.ARROW, text("gui.next"),
                    List.of(text("gui.page", "page", String.valueOf(page + 2), "pages", String.valueOf(pageCount())))));
        }
        if (canCreate()) {
            inventory.setItem(CREATE, Items.item(Material.ORANGE_BED, text("gui.create"), List.of(text("gui.create-lore"))));
        }
        refreshInfo();
    }

    private void drawFrame() {
        ItemStack accent = Items.pane(isOwn() ? Material.ORANGE_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE);
        ItemStack neutral = Items.pane(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (!HOME_SLOTS.contains(slot)) {
                inventory.setItem(slot, ACCENT_FRAME.contains(slot) ? accent : neutral);
            }
        }
    }

    private void refreshInfo() {
        if (viewer.getOpenInventory().getTopInventory().getHolder(false) != this && refresher != null) {
            refresher.cancel(); // menu fermé
            return;
        }
        List<Component> lore = new ArrayList<>();
        String count = String.valueOf(homes.size());
        if (isOwn()) {
            lore.add(text("gui.info.count", "count", count,
                    "limit", limit < 0 ? messages.plain(viewer, "gui.unlimited") : String.valueOf(limit)));
            if (limit > 0) {
                int filled = (int) Math.round(Math.min(1, (double) homes.size() / limit) * GAUGE_SIZE);
                lore.add(text("gui.info.gauge", "filled", "▰".repeat(filled), "empty", "▱".repeat(GAUGE_SIZE - filled)));
            }
            lore.add(Component.empty());
            long combat = gui.combatSeconds(viewer);
            long cooldown = Math.max(0, (cooldownUntil - System.currentTimeMillis() + 999) / 1000);
            if (combat > 0) {
                lore.add(text("gui.info.combat", "time", String.valueOf(combat)));
            } else if (cooldown > 0) {
                lore.add(text("gui.info.cooldown", "time", String.valueOf(cooldown)));
            } else {
                lore.add(text("gui.info.ready"));
            }
        } else {
            lore.add(text("gui.info.count-other", "count", count));
            lore.add(Component.empty());
            lore.add(text("gui.info.admin"));
        }
        inventory.setItem(INFO, Items.head(isOwn() ? viewer.getPlayerProfile() : Bukkit.createProfile(owner, ownerName),
                text("gui.info.name", "player", ownerName), lore));
    }

    private ItemStack homeItem(Home home) {
        boolean here = home.isOn(gui.serverName());
        List<Component> lore = new ArrayList<>();
        lore.add(text(here ? "gui.home.server-here" : "gui.home.server", "server", home.getServer()));
        lore.add(text("gui.home.world", "world", home.getWorld()));
        lore.add(text("gui.home.coords", "x", coordinate(home.getX()), "y", coordinate(home.getY()), "z", coordinate(home.getZ())));
        lore.add(Component.empty());

        HomePermissions permissions = gui.permissions();
        if (permissions.canTeleport(viewer, owner)) {
            lore.add(text("gui.home.teleport"));
        }
        if (permissions.canRename(viewer, owner)) {
            lore.add(text("gui.home.edit"));
        }
        if (permissions.canDelete(viewer, owner)) {
            lore.add(text("gui.home.delete"));
        }
        // Brillant : home sur ce serveur, téléportation directe
        return Items.item(home.getDisplayIcon(gui.serverName()), text("gui.home.name", "home", home.getName()), lore, here);
    }

    private Component text(String key, String... placeholders) {
        return messages.get(viewer, key, placeholders);
    }

    private static String coordinate(double value) {
        return String.valueOf(Math.round(value));
    }

    private boolean isOwn() {
        return viewer.getUniqueId().equals(owner);
    }

    private int pageCount() {
        return Math.max(1, (homes.size() + HOME_SLOTS.size() - 1) / HOME_SLOTS.size());
    }

    private boolean hasNextPage() {
        return page + 1 < pageCount();
    }

    /** Uniquement pour ses propres homes : un admin ne crée pas de home à la place d'un autre. */
    private boolean canCreate() {
        return isOwn() && viewer.hasPermission(HomePermissions.SET);
    }

    /** Lignes 2 à 5, colonnes 2 à 8 : l'intérieur du cadre. */
    private static List<Integer> homeSlots() {
        List<Integer> slots = new ArrayList<>();
        for (int row = 1; row <= 4; row++) {
            for (int column = 1; column <= 7; column++) {
                slots.add(row * 9 + column);
            }
        }
        return List.copyOf(slots);
    }
}

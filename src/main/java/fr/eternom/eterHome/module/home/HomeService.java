package fr.eternom.eterHome.module.home;

import fr.eternom.eterHome.api.HomeApi;
import fr.eternom.eterHome.module.gui.HomeGui;
import fr.eternom.eterHome.module.permission.HomePermissions;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/** L'API d'EterHome (HomeApi) : le plugin lui-même, vu de l'extérieur. */
public class HomeService implements HomeApi {

    private final HomeManager homes;
    private final HomeActions actions;
    private final HomePermissions permissions;
    private final HomeGui gui;

    public HomeService(HomeManager homes, HomeActions actions, HomePermissions permissions, HomeGui gui) {
        this.homes = homes;
        this.actions = actions;
        this.permissions = permissions;
        this.gui = gui;
    }

    @Override
    public List<HomeInfo> homes(UUID owner) {
        return homes.getAll(owner).stream().map(home -> new HomeInfo(home.getName(), home.getServer(), home.getWorld(), home.getX(),
                home.getY(), home.getZ(), home.getYaw(), home.getPitch(), home.getIcon())).toList();
    }

    @Override
    public int limit(Player player) {
        return permissions.limit(player);
    }

    @Override
    public void openMenu(Player viewer, UUID owner, String ownerName) {
        gui.open(viewer, owner, ownerName);
    }

    @Override
    public void teleport(Player player, UUID owner, String name) {
        if (permissions.canTeleport(player, owner)) {
            actions.teleportHome(player, owner, name);
        }
    }
}

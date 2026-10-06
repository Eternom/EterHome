package fr.eternom.eterHome.module.permission;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.UUID;

/**
 * Règles d'accès aux homes. Les permissions passent par Bukkit (donc LuckPerms ou autre),
 * elles ne concernent que des joueurs en ligne : pas besoin de Vault.
 */
public class HomePermissions {

    public static final String SET = "eterhome.set";
    public static final String DELETE = "eterhome.delete";
    public static final String RENAME = "eterhome.rename";
    public static final String TELEPORT = "eterhome.teleport";
    public static final String LIST = "eterhome.list";
    public static final String OTHERS_VIEW = "eterhome.others.view";
    public static final String OTHERS_TELEPORT = "eterhome.others.teleport";
    public static final String OTHERS_DELETE = "eterhome.others.delete";
    public static final String OTHERS_RENAME = "eterhome.others.rename";
    public static final String UNLIMITED = "eterhome.limit.unlimited";
    private static final String LIMIT_PREFIX = "eterhome.limit.";

    private final int defaultLimit;

    public HomePermissions(int defaultLimit) {
        this.defaultLimit = defaultLimit;
    }

    /** Nombre max de homes : le plus grand eterhome.limit.<n> du joueur, sans descendre sous la valeur par défaut. -1 = illimité. */
    public int limit(Player player) {
        if (player.hasPermission(UNLIMITED)) {
            return -1;
        }
        int limit = defaultLimit;
        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            String permission = info.getPermission();
            if (info.getValue() && permission.startsWith(LIMIT_PREFIX)) {
                try {
                    limit = Math.max(limit, Integer.parseInt(permission.substring(LIMIT_PREFIX.length())));
                } catch (NumberFormatException ignored) {
                    // eterhome.limit.* ou autre valeur non numérique
                }
            }
        }
        return limit;
    }

    public boolean canTeleport(Player viewer, UUID owner) {
        return viewer.hasPermission(isSelf(viewer, owner) ? TELEPORT : OTHERS_TELEPORT);
    }

    public boolean canDelete(Player viewer, UUID owner) {
        return viewer.hasPermission(isSelf(viewer, owner) ? DELETE : OTHERS_DELETE);
    }

    public boolean canRename(Player viewer, UUID owner) {
        return viewer.hasPermission(isSelf(viewer, owner) ? RENAME : OTHERS_RENAME);
    }

    private boolean isSelf(Player viewer, UUID owner) {
        return viewer.getUniqueId().equals(owner);
    }
}

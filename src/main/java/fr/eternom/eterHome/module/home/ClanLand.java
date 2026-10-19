package fr.eternom.eterHome.module.home;

import fr.eternom.eterClan.api.ClanApi;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Terrains des clans (EterClan, facultatif) : pas de home sur le terrain d'un autre clan. Seuls les terrains de CE
 * serveur sont connus (EterClan tourne aussi sur les lobbys, sans terrain). Thread principal.
 */
public final class ClanLand {

    private ClanLand() {
    }

    /** Le joueur peut avoir un home là : hors terrain, sur le terrain de son clan avec la permission « homes », sans EterClan. */
    public static boolean allows(Player player, Location location) {
        return location == null || !Bukkit.getPluginManager().isPluginEnabled("EterClan") || Api.canHome(player, location);
    }

    /** Seule classe qui touche à ClanApi : jamais chargée sans EterClan (sinon NoClassDefFoundError). */
    private static final class Api {
        static boolean canHome(Player player, Location location) {
            return ClanApi.get().map(api -> api.canHome(player, location)).orElse(true);
        }
    }
}

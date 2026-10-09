package fr.eternom.eterHome.api;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Ce qu'EterHome offre aux autres plugins : les homes d'un joueur, sa limite, y aller, et le menu des homes (vue staff
 * comprise). Personne d'autre ne lit eterhome_homes ni la copie Redis : on demande ici.
 * <pre>
 *     // compileOnly("com.github.Eternom:EterHome:&lt;tag&gt;") ; plugin.yml : softdepend: [EterHome]
 *     HomeApi.get().ifPresent(homes -> homes.openMenu(staff, target, targetName));
 * </pre>
 */
public interface HomeApi {

    /** Un home, sur n'importe quel serveur du réseau. */
    record HomeInfo(String name, String server, String world, double x, double y, double z, float yaw, float pitch,
                    String icon) {
    }

    /** L'API d'EterHome si le plugin tourne sur ce serveur. */
    static Optional<HomeApi> get() {
        return Optional.ofNullable(Bukkit.getServicesManager().load(HomeApi.class));
    }

    /** Les homes d'un joueur (connecté ou non). Bloquant (Redis, sinon base) : hors du thread principal. */
    List<HomeInfo> homes(UUID owner);

    /** Combien de homes ce joueur connecté peut avoir (permissions) ; -1 = sans limite. */
    int limit(Player player);

    /** Le menu des homes de owner, pour viewer (ses homes, ou ceux d'un autre avec eterhome.others.view). Thread principal. */
    void openMenu(Player viewer, UUID owner, String ownerName);

    /** Aller au home name de owner avec les règles habituelles (permissions, combat, délai, attente) ; sans la permission : rien. Thread principal. */
    void teleport(Player player, UUID owner, String name);
}

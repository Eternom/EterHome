package fr.eternom.eterHome.module.home;

import com.destroystokyo.paper.event.player.PlayerSetSpawnEvent;
import fr.eternom.eterLib.helper.message.Messages;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Un lit (ou une ancre) devient le point de réapparition : on rappelle qu'un home y ramène de partout (/sethome).
 * Une fois par connexion, pour ne pas répéter à chaque nuit.
 */
public class BedHint implements Listener {

    private final Messages messages;
    private final Set<UUID> told = ConcurrentHashMap.newKeySet();

    public BedHint(Messages messages) {
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSetSpawn(PlayerSetSpawnEvent event) {
        if ((event.getCause() == PlayerSetSpawnEvent.Cause.BED || event.getCause() == PlayerSetSpawnEvent.Cause.RESPAWN_ANCHOR)
                && told.add(event.getPlayer().getUniqueId())) {
            messages.send(event.getPlayer(), "home.bed-hint");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        told.remove(event.getPlayer().getUniqueId());
    }
}

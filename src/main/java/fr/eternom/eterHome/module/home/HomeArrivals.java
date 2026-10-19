package fr.eternom.eterHome.module.home;

import fr.eternom.eterLib.helper.cache.RedisCache;
import fr.eternom.eterLib.helper.gui.Sounds;
import fr.eternom.eterLib.helper.message.Messages;
import fr.eternom.eterLib.helper.task.Tasks;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;

/**
 * Un home sur un AUTRE serveur ne peut pas être vérifié au départ (les terrains de clan ne sont connus que sur leur
 * serveur) : le départ est noté dans Redis, et à l'arrivée, un home sur le terrain d'un autre clan renvoie au lobby
 * (canal eter:lobby d'EterVelocityLobby ; sans lui, au spawn du monde).
 */
public class HomeArrivals implements Listener {

    private static final String KEY = "eterhome:arrival:";
    /** Le temps d'arriver (attente, changement de serveur) ; au-delà, la note est oubliée. */
    private static final Duration TTL = Duration.ofMinutes(1);
    private static final String LOBBY_CHANNEL = "eter:lobby";

    private final JavaPlugin plugin;
    private final RedisCache redis;
    private final Messages messages;

    public HomeArrivals(JavaPlugin plugin, RedisCache redis, Messages messages) {
        this.plugin = plugin;
        this.redis = redis;
        this.messages = messages;
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, LOBBY_CHANNEL);
    }

    /** Le joueur part vers un de ses homes sur un autre serveur (thread principal, au départ). */
    public void departing(Player player) {
        String key = KEY + player.getUniqueId();
        Tasks.async(plugin, () -> redis.set(key, "1", TTL), "Arrivée au home non notée pour " + player.getName());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String key = KEY + player.getUniqueId();
        Tasks.async(plugin, player, () -> {
            boolean arriving = redis.get(key).isPresent();
            if (arriving) {
                redis.delete(key);
            }
            return arriving;
        }, arriving -> {
            if (arriving && !ClanLand.allows(player, player.getLocation())) {
                expel(player);
            }
        }, () -> { });
    }

    private void expel(Player player) {
        messages.send(player, "home.clan-land-arrival");
        Sounds.refuse(player);
        if (player.getListeningPluginChannels().contains(LOBBY_CHANNEL)) {
            player.sendPluginMessage(plugin, LOBBY_CHANNEL, new byte[0]);
        } else {
            player.teleportAsync(player.getWorld().getSpawnLocation());
        }
    }
}

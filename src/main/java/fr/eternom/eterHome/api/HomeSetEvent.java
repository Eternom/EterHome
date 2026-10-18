package fr.eternom.eterHome.api;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Un joueur vient de créer ou de déplacer un de ses homes (thread principal) : la base l'a déjà enregistré. Pour les
 * autres plugins, sans qu'EterHome les connaisse (ex : l'étape « Un home » des Premiers pas d'EterSuccess, qui n'attend
 * plus sa relecture de chaque minute).
 * <pre>
 *     // compileOnly("com.github.Eternom:EterHome:&lt;tag&gt;") ; plugin.yml : softdepend: [EterHome]
 *     // Écouteur dans une classe à part, enregistrée seulement si EterHome est présent
 * </pre>
 */
public class HomeSetEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String home;
    private final boolean created;

    public HomeSetEvent(Player player, String home, boolean created) {
        super(player);
        this.home = home;
        this.created = created;
    }

    /** Nom du home (normalisé). */
    public String getHome() {
        return home;
    }

    /** true pour un nouveau home, false pour un home déplacé. */
    public boolean isCreated() {
        return created;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

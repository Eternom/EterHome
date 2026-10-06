package fr.eternom.eterHome.module.gui;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** Sons d'interface, joués uniquement pour le joueur concerné. */
final class Sounds {

    private Sounds() {
    }

    static void click(Player player) {
        player.playSound(player, Sound.UI_BUTTON_CLICK, 0.4f, 1.2f);
    }

    static void page(Player player) {
        player.playSound(player, Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1f);
    }
}

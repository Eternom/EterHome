package fr.eternom.eterHome.module.gui;

import fr.eternom.eterLib.helper.gui.Dialogs;
import fr.eternom.eterLib.helper.message.Messages;
import fr.eternom.eterHome.module.home.Home;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.function.Consumer;

/**
 * Fenêtres natives de Minecraft (Dialogs, client 1.21.6+) : créer, modifier et supprimer un home.
 * Boutons et thread principal : Dialogs d'EterLib. « Annuler » appelle onCancel (retour au menu).
 */
public class HomeDialogs {

    private static final String NAME = "name";
    private static final String USE_HAND = "use_hand";

    /** Ce que le joueur a validé : le nom, et l'item en main si la case « icône » est cochée. */
    public record HomeForm(String name, Material icon) {
    }

    private final JavaPlugin plugin;
    private final Messages messages;

    public HomeDialogs(JavaPlugin plugin, Messages messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    public void create(Player player, String suggestedName, Consumer<HomeForm> onConfirm, Runnable onCancel) {
        form(player, "dialog.create", suggestedName, onConfirm, onCancel);
    }

    public void edit(Player player, Home home, Consumer<HomeForm> onConfirm, Runnable onCancel) {
        form(player, "dialog.edit", home.getName(), onConfirm, onCancel);
    }

    public void confirmDelete(Player player, Home home, Material icon, Runnable onConfirm, Runnable onCancel) {
        DialogBase base = DialogBase.builder(messages.get(player, "dialog.delete.title"))
                .body(List.of(
                        DialogBody.item(ItemStack.of(icon)).showTooltip(false).build(),
                        DialogBody.plainMessage(messages.get(player, "dialog.delete.body", "home", home.getName()))))
                .build();
        Dialogs.show(plugin, player, base, messages.get(player, "dialog.delete.confirm"), messages.get(player, "dialog.cancel"),
                response -> onConfirm.run(), onCancel);
    }

    /** Formulaire commun à la création et à la modification : un champ nom et une case « item en main comme icône ». */
    private void form(Player player, String keyPrefix, String initialName, Consumer<HomeForm> onConfirm, Runnable onCancel) {
        DialogBase base = DialogBase.builder(messages.get(player, keyPrefix + ".title"))
                .body(List.of(DialogBody.plainMessage(messages.get(player, keyPrefix + ".body"))))
                .inputs(List.of(
                        DialogInput.text(NAME, messages.get(player, "dialog.name"))
                                .initial(initialName)
                                .maxLength(32)
                                .build(),
                        DialogInput.bool(USE_HAND, messages.get(player, "dialog.use-hand")).build()))
                .build();
        Dialogs.show(plugin, player, base, messages.get(player, keyPrefix + ".confirm"), messages.get(player, "dialog.cancel"),
                response -> onConfirm.accept(readForm(player, response)), onCancel);
    }

    private HomeForm readForm(Player player, DialogResponseView response) {
        String name = response.getText(NAME);
        Material icon = null;
        if (Boolean.TRUE.equals(response.getBoolean(USE_HAND))) {
            Material hand = player.getInventory().getItemInMainHand().getType();
            icon = hand.isAir() ? null : hand;
        }
        return new HomeForm(name == null ? "" : name.trim(), icon);
    }

}

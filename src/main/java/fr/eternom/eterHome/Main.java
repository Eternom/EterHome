package fr.eternom.eterHome;

import fr.eternom.eterHome.listeners.Commands;
import fr.eternom.eterHome.listeners.Events;
import fr.eternom.eterHome.module.gui.HomeDialogs;
import fr.eternom.eterHome.module.gui.HomeGui;
import fr.eternom.eterHome.module.home.HomeActions;
import fr.eternom.eterHome.module.home.HomeManager;
import fr.eternom.eterHome.module.permission.HomePermissions;
import fr.eternom.eterLib.EterLib;
import fr.eternom.eterLib.helper.message.Messages;
import fr.eternom.eterLib.helper.sql.Database;
import fr.eternom.eterLib.module.player.PlayerDirectory;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    /** Préfixe des tables d'EterHome dans la base commune : eterhome_homes. */
    private static final String TABLE_PREFIX = "eterhome_";

    private static Main instance;
    private Messages messages;
    private HomeManager homeManager;
    private PlayerDirectory playerDirectory;
    private HomeActions homeActions;
    private HomeGui homeGui;

    @Override
    public void onEnable() {

        instance = this;
        saveDefaultConfig();

        // Connexions, serveur, langue, joueurs et téléportation : fournis par EterLib (depend: [EterLib])
        EterLib lib = EterLib.get();
        String serverName = lib.getServerName();
        Database database = lib.database(TABLE_PREFIX);
        messages = lib.messages(this, "en_us", "fr_fr");
        playerDirectory = lib.getPlayers();

        HomePermissions permissions = new HomePermissions(getConfig().getInt("homes.default-limit", 3));
        homeManager = new HomeManager(database, lib.getRedis());
        homeActions = new HomeActions(this, homeManager, lib.getTeleports(), permissions, messages, serverName);
        homeGui = new HomeGui(this, homeActions, homeManager, permissions, new HomeDialogs(this, messages),
                lib.getTeleports(), messages, serverName);

        new Commands(this);
        new Events(this);
    }

    public Messages getMessages() {
        return messages;
    }

    public HomeManager getHomeManager() {
        return homeManager;
    }

    public PlayerDirectory getPlayerDirectory() {
        return playerDirectory;
    }

    public HomeActions getHomeActions() {
        return homeActions;
    }

    public HomeGui getHomeGui() {
        return homeGui;
    }

    public static Main getInstance() {
        return instance;
    }
}

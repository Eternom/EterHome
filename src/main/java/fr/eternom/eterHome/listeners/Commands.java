package fr.eternom.eterHome.listeners;

import fr.eternom.eterHome.Main;
import fr.eternom.eterHome.module.home.HomeCommand;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;

import java.util.Objects;

public class Commands {

    public Commands(Main main) {
        HomeCommand home = new HomeCommand(main.getHomeActions(), main.getHomeGui(), main.getPlayerDirectory(), main.getMessages());
        register(main, "sethome", home);
        register(main, "delhome", home);
        register(main, "home", home);
        register(main, "homes", home);
    }

    private void register(Main main, String name, TabExecutor executor) {
        PluginCommand command = Objects.requireNonNull(main.getCommand(name), "Commande absente du plugin.yml : " + name);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

}

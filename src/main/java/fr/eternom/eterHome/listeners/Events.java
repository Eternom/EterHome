package fr.eternom.eterHome.listeners;

import fr.eternom.eterHome.Main;
import fr.eternom.eterHome.module.gui.MenuListener;
import fr.eternom.eterHome.module.home.HomeTabListener;
import org.bukkit.event.Listener;

public class Events {

    public Events(Main main) {
        register(main, new MenuListener(main));

        // Sans Redis, compléter lirait SQL à chaque touche : désactivé
        if (main.getHomeManager().hasCache()) {
            register(main, new HomeTabListener(main.getHomeManager()));
        }
    }

    private void register(Main main, Listener listener) {
        main.getServer().getPluginManager().registerEvents(listener, main);
    }

}

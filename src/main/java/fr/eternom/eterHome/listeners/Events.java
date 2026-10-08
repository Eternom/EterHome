package fr.eternom.eterHome.listeners;

import fr.eternom.eterHome.Main;
import fr.eternom.eterHome.module.home.HomeTabListener;
import org.bukkit.event.Listener;

public class Events {

    public Events(Main main) {
        // Les clics dans les menus sont gérés par EterLib, pour tous les plugins Eter.
        // Complétion des noms de homes : lue dans la copie Redis, jamais en base à chaque touche
        register(main, new HomeTabListener(main.getHomeManager()));
    }

    private void register(Main main, Listener listener) {
        main.getServer().getPluginManager().registerEvents(listener, main);
    }

}

package fr.eternom.eterHome;

import fr.eternom.eterHome.core.Cache;
import fr.eternom.eterHome.core.Sql;
import fr.eternom.eterHome.helper.sql.ISql;
import fr.eternom.eterHome.listeners.Commands;
import fr.eternom.eterHome.listeners.Events;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    private Main instance;
    private Sql sql;
    private Cache cache;

    @Override
    public void onEnable() {

        instance = this;

        saveDefaultConfig();
        sql = new Sql(this);
        sql.connect();
        cache = new Cache(this);
        cache.connect();

        new Commands(this);
        new Events(this);
    }

    @Override
    public void onDisable() {
        if (cache != null) {
            cache.close();
        }
        if (sql != null) {
            sql.close();
        }
    }

    public Sql getSql() {
        return sql;
    }

    public Cache getCache() {
        return cache;
    }

    public ISql getDatabase() {
        return sql.getDatabase();
    }


    public Main getInstance() {
        return instance;
    }
}

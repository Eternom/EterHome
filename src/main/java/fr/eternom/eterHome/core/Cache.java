package fr.eternom.eterHome.core;

import fr.eternom.eterHome.helper.cache.ICache;
import fr.eternom.eterHome.helper.cache.MemoryCache;
import fr.eternom.eterHome.helper.cache.RedisCache;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Locale;

/**
 * Cycle de vie du cache : init, getJedis, close.
 * Le cache est optionnel : toujours vérifier {@link #isEnabled()} avant {@link #getStore()},
 * et lire la base de données sinon.
 */
public class Cache {

    public enum Type { NONE, MEMORY, REDIS }

    private final JavaPlugin plugin;
    private Type type = Type.NONE;
    private JedisPool jedisPool;
    private String prefix = "";
    private ICache store;

    public Cache(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("cache");
        type = section == null ? Type.NONE
                : Type.valueOf(section.getString("type", "none").toUpperCase(Locale.ROOT));

        switch (type) {
            case NONE -> store = null;
            case MEMORY -> store = new MemoryCache();
            case REDIS -> {
                ConfigurationSection redis = section.getConfigurationSection("redis");
                if (redis == null) {
                    throw new IllegalStateException("Section 'cache.redis' manquante dans config.yml");
                }

                // Reconnexion gérée par le pool : chaque connexion est testée (PING) avant d'être prêtée
                // et les connexions inactives sont vérifiées régulièrement.
                JedisPoolConfig poolConfig = new JedisPoolConfig();
                poolConfig.setMaxTotal(redis.getInt("pool-size", 8));
                poolConfig.setTestOnBorrow(true);
                poolConfig.setTestWhileIdle(true);

                String password = redis.getString("password", "");
                jedisPool = new JedisPool(poolConfig,
                        redis.getString("host", "localhost"),
                        redis.getInt("port", 6379),
                        2000,
                        password.isEmpty() ? null : password,
                        redis.getInt("database", 0));
                prefix = redis.getString("prefix", "eterhome:");

                // Échoue dès le démarrage si Redis est injoignable
                try (Jedis jedis = jedisPool.getResource()) {
                    jedis.ping();
                }
                store = new RedisCache(this);
            }
        }

        plugin.getLogger().info("Cache : " + type.name().toLowerCase(Locale.ROOT));
    }

    public boolean isEnabled() {
        return store != null;
    }

    public ICache getStore() {
        if (store == null) {
            throw new IllegalStateException("Le cache est désactivé : vérifier isEnabled() avant");
        }
        return store;
    }

    public Jedis getJedis() {
        if (jedisPool == null || jedisPool.isClosed()) {
            throw new IllegalStateException("Le pool Redis n'est pas démarré");
        }
        return jedisPool.getResource();
    }

    public String getPrefix() {
        return prefix;
    }

    public Type getType() {
        return type;
    }

    public void close() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
        store = null;
    }
}

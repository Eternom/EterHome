package fr.eternom.eterHome.core;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.eternom.eterHome.helper.sql.H2;
import fr.eternom.eterHome.helper.sql.ISql;
import fr.eternom.eterHome.helper.sql.MariaDb;
import fr.eternom.eterHome.helper.sql.MySql;
import fr.eternom.eterHome.helper.sql.PostgreSql;
import fr.eternom.eterHome.helper.sql.SQLite;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Cycle de vie de la connexion : init du pool, getConnection, close.
 * Les requêtes passent par {@link #getDatabase()}, adapté au type de base.
 */
public class Sql {

    public enum Type {
        MYSQL("com.mysql.cj.jdbc.Driver", 3306),
        MARIADB("org.mariadb.jdbc.Driver", 3306),
        POSTGRESQL("org.postgresql.Driver", 5432),
        SQLITE("org.sqlite.JDBC", 0),
        H2("org.h2.Driver", 0);

        private final String driver;
        private final int defaultPort;

        Type(String driver, int defaultPort) {
            this.driver = driver;
            this.defaultPort = defaultPort;
        }
    }

    private final JavaPlugin plugin;
    private HikariDataSource dataSource;
    private Type type;
    private ISql database;

    public Sql(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("database");
        if (section == null) {
            throw new IllegalStateException("Section 'database' manquante dans config.yml");
        }

        type = Type.valueOf(section.getString("type", "sqlite").toUpperCase(Locale.ROOT));

        HikariConfig config = new HikariConfig();
        config.setPoolName("EterHome-" + type.name());
        config.setDriverClassName(type.driver);

        switch (type) {
            case SQLITE -> {
                config.setJdbcUrl("jdbc:sqlite:" + databaseFile(section).getAbsolutePath() + ".db");
                // SQLite n'accepte qu'un seul écrivain à la fois
                config.setMaximumPoolSize(1);
            }
            case H2 -> {
                config.setJdbcUrl("jdbc:h2:file:" + databaseFile(section).getAbsolutePath());
                config.setMaximumPoolSize(4);
            }
            default -> {
                int port = section.getInt("port", 0);
                config.setJdbcUrl("jdbc:%s://%s:%d/%s".formatted(type.name().toLowerCase(Locale.ROOT),
                        section.getString("host", "localhost"), port > 0 ? port : type.defaultPort,
                        section.getString("name", "eterhome")));
                config.setUsername(section.getString("username"));
                config.setPassword(section.getString("password"));
                config.setMaximumPoolSize(section.getInt("pool-size", 10));
            }
        }

        // Reconnexion gérée par Hikari : chaque connexion est validée avant d'être prêtée,
        // les connexions mortes sont remplacées, et elles sont renouvelées avant que la base
        // ne les coupe d'elle-même (wait_timeout MySQL = 8h par défaut).
        config.setMaxLifetime(TimeUnit.MINUTES.toMillis(30));
        config.setKeepaliveTime(TimeUnit.MINUTES.toMillis(5));
        config.setConnectionTimeout(TimeUnit.SECONDS.toMillis(10));

        dataSource = new HikariDataSource(config);

        database = switch (type) {
            case MYSQL -> new MySql(this);
            case MARIADB -> new MariaDb(this);
            case POSTGRESQL -> new PostgreSql(this);
            case SQLITE -> new SQLite(this);
            case H2 -> new H2(this);
        };

        plugin.getLogger().info("Connecté à la base de données (" + type.name().toLowerCase(Locale.ROOT) + ")");
    }

    public Connection getConnection() throws SQLException {
        if (!isConnected()) {
            throw new SQLException("Le pool de connexions n'est pas démarré");
        }
        return dataSource.getConnection();
    }

    public boolean isConnected() {
        return dataSource != null && dataSource.isRunning();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public Type getType() {
        return type;
    }

    public ISql getDatabase() {
        return database;
    }

    private File databaseFile(ConfigurationSection section) {
        plugin.getDataFolder().mkdirs();
        return new File(plugin.getDataFolder(), section.getString("file", "database"));
    }
}

package fr.eternom.eterHome.module.home;

import fr.eternom.eterLib.helper.cache.RedisCache;
import fr.eternom.eterLib.helper.sql.Column;
import fr.eternom.eterLib.helper.sql.Database;

import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Homes des joueurs, partagés entre tous les serveurs.
 * SQL est la source de vérité. Redis en garde une copie : hash "homes:<uuid>", un champ par home,
 * supprimée à chaque modification et rechargée depuis SQL à la lecture suivante.
 * Les appels sont bloquants : à exécuter hors du thread principal.
 */
public class HomeManager {

    private static final String TABLE = "homes";
    /** Durée de vie de la copie Redis : les joueurs partis ne restent pas en mémoire. */
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    private final Database database;
    private final RedisCache redis;

    public HomeManager(Database database, RedisCache redis) {
        this.database = database;
        this.redis = redis;

        database.createTable(TABLE,
                Column.of("owner", Column.Type.UUID).primaryKey(),
                Column.of("name", Column.Type.STRING).length(32).primaryKey(),
                Column.of("server", Column.Type.STRING).length(64).notNull(),
                Column.of("world", Column.Type.STRING).length(64).notNull(),
                Column.of("x", Column.Type.DOUBLE).notNull(),
                Column.of("y", Column.Type.DOUBLE).notNull(),
                Column.of("z", Column.Type.DOUBLE).notNull(),
                Column.of("yaw", Column.Type.FLOAT).notNull(),
                Column.of("pitch", Column.Type.FLOAT).notNull(),
                Column.of("icon", Column.Type.STRING).length(64));
        // Tables créées avant l'ajout des icônes
        database.addColumn(TABLE, Column.of("icon", Column.Type.STRING).length(64));
    }

    public Optional<Home> get(UUID owner, String name) {
        String home = Home.normalize(name);
        return Optional.ofNullable(cached(owner).get(home)).map(value -> Home.deserialize(owner, home, value));
    }

    /** Tous les homes du joueur, tous serveurs confondus, triés par nom. */
    public List<Home> getAll(UUID owner) {
        return cached(owner).entrySet().stream()
                .map(entry -> Home.deserialize(owner, entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(Home::getName))
                .toList();
    }

    /** Crée le home, ou le remplace s'il existe déjà (même s'il était sur un autre serveur). */
    public void set(Home home) {
        database.set(TABLE, home.toMap(), "owner", "name");
        invalidate(home.getOwner());
    }

    public enum SetResult { CREATED, REPLACED, LIMIT_REACHED }

    /**
     * Comme {@link #set(Home)}, mais refuse un NOUVEAU home au-delà de limit (-1 = illimité). Remplacer reste permis,
     * et garde l'icône choisie si le nouveau home n'en précise pas.
     */
    public SetResult set(Home home, int limit) {
        List<Home> homes = getAll(home.getOwner());
        Optional<Home> existing = homes.stream().filter(other -> other.getName().equals(home.getName())).findFirst();
        if (existing.isEmpty() && limit >= 0 && homes.size() >= limit) {
            return SetResult.LIMIT_REACHED;
        }
        set(home.getIcon() == null ? home.withIcon(existing.map(Home::getIcon).orElse(null)) : home);
        return existing.isPresent() ? SetResult.REPLACED : SetResult.CREATED;
    }

    /** @return false si le home n'existe pas */
    public boolean setIcon(UUID owner, String name, String icon) {
        boolean updated = database.update(TABLE, Map.of("icon", icon), Map.of("owner", owner, "name", Home.normalize(name))) > 0;
        if (updated) {
            invalidate(owner);
        }
        return updated;
    }

    /** Premier nom libre parmi home1, home2... (bouton « Définir un home ici » du GUI). */
    public String nextFreeName(UUID owner) {
        List<String> taken = getAll(owner).stream().map(Home::getName).toList();
        int index = 1;
        while (taken.contains("home" + index)) {
            index++;
        }
        return "home" + index;
    }

    public enum RenameResult { RENAMED, NOT_FOUND, NAME_TAKEN }

    public RenameResult rename(UUID owner, String oldName, String newName) {
        String from = Home.normalize(oldName);
        String to = Home.normalize(newName);
        if (get(owner, to).isPresent()) {
            return RenameResult.NAME_TAKEN;
        }
        int updated = database.update(TABLE, Map.of("name", to), Map.of("owner", owner, "name", from));
        if (updated == 0) {
            return RenameResult.NOT_FOUND;
        }
        invalidate(owner);
        return RenameResult.RENAMED;
    }

    /** @return true si un home a bien été supprimé */
    public boolean delete(UUID owner, String name) {
        boolean deleted = database.delete(TABLE, Map.of("owner", owner, "name", Home.normalize(name))) > 0;
        if (deleted) {
            invalidate(owner);
        }
        return deleted;
    }

    /** Homes du joueur depuis Redis (nom -> home sérialisé), rechargés depuis SQL s'ils n'y sont pas. */
    private Map<String, String> cached(UUID owner) {
        Map<String, String> cached = redis.getHash(key(owner));
        if (!cached.isEmpty()) {
            return cached;
        }

        // Un joueur sans home n'a pas de hash (Redis ne garde pas les hash vides) : ce cas relit SQL à chaque fois
        Map<String, String> homes = new HashMap<>();
        database.get(TABLE, Map.of("owner", owner)).stream()
                .map(Home::fromRow)
                .forEach(home -> homes.put(home.getName(), home.serialize()));
        if (!homes.isEmpty()) {
            redis.setHash(key(owner), homes);
            redis.expire(key(owner), CACHE_TTL);
        }
        return homes;
    }

    /** Après une écriture SQL : la copie Redis est obsolète, la prochaine lecture la recharge. */
    private void invalidate(UUID owner) {
        redis.delete(key(owner));
    }

    private String key(UUID owner) {
        // v2 : format avec icône ; les copies de l'ancien format expirent seules (TTL)
        return "homes:v2:" + owner;
    }
}

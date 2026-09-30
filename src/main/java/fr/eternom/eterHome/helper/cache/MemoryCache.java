package fr.eternom.eterHome.helper.cache;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache en mémoire du serveur. Rapide, mais propre à ce serveur et vidé au redémarrage.
 * Thread-safe : utilisable depuis les tâches async.
 */
public class MemoryCache implements ICache {

    private record Entry(String value, long expiresAt) {
        boolean isExpired() {
            return expiresAt > 0 && System.currentTimeMillis() > expiresAt;
        }
    }

    private final Map<String, Entry> values = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> hashes = new ConcurrentHashMap<>();

    @Override
    public Optional<String> get(String key) {
        Entry entry = values.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.isExpired()) {
            values.remove(key, entry);
            return Optional.empty();
        }
        return Optional.of(entry.value());
    }

    @Override
    public void set(String key, String value) {
        values.put(key, new Entry(value, 0));
    }

    @Override
    public void set(String key, String value, Duration ttl) {
        values.put(key, new Entry(value, System.currentTimeMillis() + ttl.toMillis()));
    }

    @Override
    public void delete(String key) {
        values.remove(key);
        hashes.remove(key);
    }

    @Override
    public boolean exists(String key) {
        return get(key).isPresent() || hashes.containsKey(key);
    }

    @Override
    public Map<String, String> getHash(String key) {
        Map<String, String> hash = hashes.get(key);
        return hash == null ? Map.of() : Map.copyOf(hash);
    }

    @Override
    public Optional<String> getHashField(String key, String field) {
        Map<String, String> hash = hashes.get(key);
        return hash == null ? Optional.empty() : Optional.ofNullable(hash.get(field));
    }

    @Override
    public void setHashField(String key, String field, String value) {
        hashes.computeIfAbsent(key, k -> new ConcurrentHashMap<>()).put(field, value);
    }

    @Override
    public void setHash(String key, Map<String, String> values) {
        hashes.put(key, new ConcurrentHashMap<>(values));
    }

    @Override
    public void deleteHashField(String key, String field) {
        hashes.computeIfPresent(key, (k, hash) -> {
            hash.remove(field);
            return hash.isEmpty() ? null : hash;
        });
    }
}

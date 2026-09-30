package fr.eternom.eterHome.helper.cache;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Cache clé/valeur indépendant de l'implémentation (mémoire ou redis).
 * Obtenu via Cache#getStore(), uniquement si Cache#isEnabled().
 *
 * Deux usages :
 * - valeur simple : get / set / delete
 * - hash (une clé contenant plusieurs champs, ex : "homes:<uuid>" -> nom du home -> position)
 */
public interface ICache {

    Optional<String> get(String key);

    void set(String key, String value);

    /** La valeur est supprimée automatiquement après ttl. */
    void set(String key, String value, Duration ttl);

    void delete(String key);

    boolean exists(String key);

    /** Tous les champs du hash (map vide si la clé n'existe pas). */
    Map<String, String> getHash(String key);

    Optional<String> getHashField(String key, String field);

    void setHashField(String key, String field, String value);

    /** Remplace tout le contenu du hash. */
    void setHash(String key, Map<String, String> values);

    void deleteHashField(String key, String field);
}

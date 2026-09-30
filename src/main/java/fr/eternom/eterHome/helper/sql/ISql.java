package fr.eternom.eterHome.helper.sql;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Requêtes indépendantes du type de base (mysql, mariadb, postgresql, sqlite, h2).
 * Obtenu via Sql#getDatabase(). Les appels sont bloquants : à exécuter hors du thread principal.
 * Les noms de tables et de colonnes sont en minuscules (a-z, 0-9, _).
 */
public interface ISql {

    void createTable(String table, Column... columns);

    /** Retourne les lignes correspondant à where (AND entre les colonnes, map vide = toutes). */
    List<Row> get(String table, Map<String, ?> where);

    Optional<Row> getFirst(String table, Map<String, ?> where);

    void insert(String table, Map<String, ?> values);

    /** Insère la ligne, ou la met à jour si une ligne avec les mêmes keys existe déjà. */
    void set(String table, Map<String, ?> values, String... keys);

    int update(String table, Map<String, ?> values, Map<String, ?> where);

    int delete(String table, Map<String, ?> where);

    /** Requête brute, pour les cas non couverts. */
    List<Row> query(String sql, Object... params);

    /** Instruction brute, pour les cas non couverts. */
    int execute(String sql, Object... params);
}

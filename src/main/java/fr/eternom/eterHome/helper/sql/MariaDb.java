package fr.eternom.eterHome.helper.sql;

import fr.eternom.eterHome.core.Sql;

import java.util.List;

public class MariaDb extends MySql {

    public MariaDb(Sql sql) {
        super(sql);
    }

    // MariaDB ne supporte pas l'alias "AS new" de MySQL
    @Override
    protected String upsertSql(String table, List<String> columns, List<String> keys) {
        return insertSql(table, columns) + " ON DUPLICATE KEY UPDATE " + updates(columns, keys, "VALUES(%s)");
    }
}

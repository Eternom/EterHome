package fr.eternom.eterHome.helper.sql;

import fr.eternom.eterHome.core.Sql;

import java.util.List;
import java.util.StringJoiner;

public class MySql extends AbstractSql {

    public MySql(Sql sql) {
        super(sql);
    }

    @Override
    protected char quote() {
        return '`';
    }

    @Override
    protected String autoIncrementColumn(Column column) {
        return id(column.getName()) + " " + columnType(column) + " AUTO_INCREMENT PRIMARY KEY";
    }

    @Override
    protected String upsertSql(String table, List<String> columns, List<String> keys) {
        return insertSql(table, columns) + " AS new ON DUPLICATE KEY UPDATE " + updates(columns, keys, "new.%s");
    }

    /** Colonnes non-clés à mettre à jour ; si aucune, réaffecte la première pour ne rien changer. */
    protected String updates(List<String> columns, List<String> keys, String valueFormat) {
        StringJoiner updates = new StringJoiner(", ");
        for (String column : columns) {
            if (!keys.contains(column)) {
                updates.add(column + " = " + valueFormat.formatted(column));
            }
        }
        return updates.length() == 0 ? keys.getFirst() + " = " + keys.getFirst() : updates.toString();
    }
}

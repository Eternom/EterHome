package fr.eternom.eterHome.helper.sql;

import fr.eternom.eterHome.core.Sql;

import java.util.List;

public class H2 extends AbstractSql {

    public H2(Sql sql) {
        super(sql);
    }

    @Override
    protected String columnType(Column column) {
        // H2 n'a pas de type TEXT
        return column.getType() == Column.Type.TEXT ? "CHARACTER VARYING" : super.columnType(column);
    }

    @Override
    protected String upsertSql(String table, List<String> columns, List<String> keys) {
        return "MERGE INTO " + id(table) + " (" + String.join(", ", columns) + ") KEY ("
                + String.join(", ", keys) + ") VALUES ("
                + String.join(", ", columns.stream().map(c -> "?").toList()) + ")";
    }
}

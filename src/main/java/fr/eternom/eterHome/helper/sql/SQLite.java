package fr.eternom.eterHome.helper.sql;

import fr.eternom.eterHome.core.Sql;

public class SQLite extends AbstractSql {

    public SQLite(Sql sql) {
        super(sql);
    }

    @Override
    protected String autoIncrementColumn(Column column) {
        return id(column.getName()) + " INTEGER PRIMARY KEY AUTOINCREMENT";
    }
}

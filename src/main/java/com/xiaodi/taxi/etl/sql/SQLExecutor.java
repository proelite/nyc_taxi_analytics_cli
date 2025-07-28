package com.xiaodi.taxi.etl.sql;

import com.xiaodi.taxi.etl.model.NormalizedColumns;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLExecutor {
    private final Statement stmt;
    private final ColumnNormalizer normalizer;

    public SQLExecutor(Statement stmt) {
        this.stmt = stmt;
        this.normalizer = new ColumnNormalizer();
    }

    public void execute(@NotNull Path file) throws SQLException {
        String path = file.toAbsolutePath().toString().replace("\\", "\\\\");
        stmt.execute(String.format(
                "CREATE TABLE temp_trips AS SELECT * FROM read_parquet('%s')", path
        ));

        try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(temp_trips)")) {
            NormalizedColumns info = normalizer.normalize(rs);
            if (info.hasTaxiType()) {
                stmt.execute(SQLBuilder.buildInsertSql(info));
            }
        } finally {
            stmt.execute("DROP TABLE IF EXISTS temp_trips");
        }
    }
}

package com.xiaodi.taxi.etl.sql;

import com.xiaodi.taxi.etl.model.NormalizedColumns;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ColumnNormalizer {
    public NormalizedColumns normalize(@NotNull ResultSet rs) throws SQLException {
        String pickup = null, dropoff = null, type = null;
        while (rs.next()) {
            String col = rs.getString("name");
            if ("tpep_pickup_datetime".equals(col)) {
                type = "yellow";
                pickup = col;
            } else if ("lpep_pickup_datetime".equals(col)) {
                type = "green";
                pickup = col;
            }
            if ("tpep_dropoff_datetime".equals(col) || "lpep_dropoff_datetime".equals(col)) {
                dropoff = col;
            }
        }
        return new NormalizedColumns(pickup, dropoff, type);
    }
}

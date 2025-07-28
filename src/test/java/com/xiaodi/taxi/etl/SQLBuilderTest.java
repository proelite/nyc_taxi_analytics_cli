package com.xiaodi.taxi.etl;

import com.xiaodi.taxi.etl.model.NormalizedColumns;
import com.xiaodi.taxi.etl.sql.SQLBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SQLBuilderTest {
    @Test
    void testCreateTripsTableSql() {
        String sql = SQLBuilder.createTripsTable();
        assertTrue(sql.startsWith("CREATE TABLE IF NOT EXISTS trips"));
        assertTrue(sql.contains("vendor_id INTEGER"));
        assertTrue(sql.contains("taxi_type VARCHAR"));
    }

    @Test
    void testBuildInsertSql() {
        NormalizedColumns info = new NormalizedColumns("pickup", "dropoff", "green");
        String sql = SQLBuilder.buildInsertSql(info);
        assertTrue(sql.contains("INSERT INTO trips"));
        assertTrue(sql.contains("pickup AS pickup_datetime"));
        assertTrue(sql.contains("dropoff AS dropoff_datetime"));
        assertTrue(sql.contains("'green' AS taxi_type"));
    }
}
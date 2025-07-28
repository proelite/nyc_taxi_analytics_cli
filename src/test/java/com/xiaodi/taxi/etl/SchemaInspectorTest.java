package com.xiaodi.taxi.etl;

import org.junit.jupiter.api.Test;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SchemaInspectorTest {
    @Test
    void testInspectWithTpepColumns() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(true, true, false);
        when(rs.getString("name"))
                .thenReturn("tpep_pickup_datetime", "tpep_dropoff_datetime");

        SchemaInspector inspector = new SchemaInspector();
        ColumnInfo info = inspector.inspect(rs);

        assertEquals("tpep_pickup_datetime", info.pickupColumn());
        assertEquals("tpep_dropoff_datetime", info.dropoffColumn());
        assertEquals("yellow", info.taxiType());
        assertTrue(info.hasTaxiType());
    }

    @Test
    void testInspectWithLpepColumns() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(true, true, false);
        when(rs.getString("name"))
                .thenReturn("lpep_pickup_datetime", "lpep_dropoff_datetime");

        SchemaInspector inspector = new SchemaInspector();
        ColumnInfo info = inspector.inspect(rs);

        assertEquals("lpep_pickup_datetime", info.pickupColumn());
        assertEquals("lpep_dropoff_datetime", info.dropoffColumn());
        assertEquals("green", info.taxiType());
        assertTrue(info.hasTaxiType());
    }
}
package com.xiaodi.taxi.etl;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class FileProcessorTest {
    @Test
    void testProcessExecutesCorrectSql() throws Exception {
        Statement stmt = mock(Statement.class);
        ResultSet rs = mock(ResultSet.class);
        when(stmt.executeQuery("PRAGMA table_info(temp_trips)"))
                .thenReturn(rs);
        // Simulate two rows: pickup and dropoff columns
        when(rs.next()).thenReturn(true, true, false);
        when(rs.getString("name"))
                .thenReturn("tpep_pickup_datetime", "tpep_dropoff_datetime");

        FileProcessor processor = new FileProcessor(stmt);
        Path file = Paths.get("data/sample.parquet");
        processor.process(file);

        // Verify parquet load
        verify(stmt).execute(startsWith("CREATE TABLE temp_trips AS SELECT * FROM read_parquet('"));
        // Verify normalized insert SQL
        verify(stmt).execute(argThat(sql ->
                sql.contains("INSERT INTO trips") &&
                        sql.contains("tpep_pickup_datetime") &&
                        sql.contains("tpep_dropoff_datetime") &&
                        sql.contains("'yellow'")
        ));
        // Verify cleanup
        verify(stmt).execute("DROP TABLE IF EXISTS temp_trips");
    }
}

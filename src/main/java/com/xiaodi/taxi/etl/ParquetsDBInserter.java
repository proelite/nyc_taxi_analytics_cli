package com.xiaodi.taxi.etl;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.util.stream.Stream;

public class ParquetsDBInserter {
    private final DirectoryScanner directoryScanner;
    private final ConnectionFactory ConnectionFactory;

    public ParquetsDBInserter(DirectoryScanner directoryScanner, ConnectionFactory ConnectionFactory) {
        this.directoryScanner = directoryScanner;
        this.ConnectionFactory = ConnectionFactory;
    }

    public void run(Path inputDir, Path outputFile) throws IOException, SQLException {
        ensureOutputDirectoryExists(outputFile);
        deleteExistingFile(outputFile);

        try (Connection conn = ConnectionFactory.getConnection("jdbc:duckdb:" + outputFile.toString());
             Statement stmt = conn.createStatement()) {

            stmt.execute(SQLBuilder.createTripsTable());

            try (Stream<Path> files = directoryScanner.listParquetFiles(inputDir)) {
                files.forEach(path -> {
                    try {
                        FileProcessor processor = new FileProcessor(stmt);
                        processor.process(path);
                    } catch (SQLException e) {
                        throw new RuntimeException("Error processing file " + path, e);
                    }
                });
            }

            System.out.println("🎉 ETL complete. DB written to: " + outputFile);
        }
    }

    private void ensureOutputDirectoryExists(@NotNull Path outputFile) throws IOException {
        Path parent = outputFile.getParent();
        if (parent != null && Files.notExists(parent)) {
            Files.createDirectories(parent);
        }
    }

    private void deleteExistingFile(Path outputFile) throws IOException {
        if (Files.exists(outputFile) && !Files.deleteIfExists(outputFile)) {
            throw new IOException("Unable to delete existing file: " + outputFile);
        }
    }

    public static void main(String[] args) {
        Path inputDir = Paths.get("parquets");
        Path outputFile = Paths.get("duck-db", "nyc_taxi_combined.duckdb");
        ParquetsDBInserter app = new ParquetsDBInserter(
                new DefaultDirectoryScanner(),
                new DefaultConnectionFactory()
        );
        try {
            app.run(inputDir, outputFile);
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }
}

// ---- Classes ----

class DefaultDirectoryScanner implements DirectoryScanner {
    @Override
    public Stream<Path> listParquetFiles(Path inputDir) throws IOException {
        return Files.list(inputDir)
                .filter(p -> p.toString().endsWith(".parquet"));
    }
}

class DefaultConnectionFactory implements ConnectionFactory {
    @Override
    public Connection getConnection(String url) throws SQLException {
        return DriverManager.getConnection(url);
    }
}

class FileProcessor {
    private final Statement stmt;
    private final SchemaInspector inspector;

    FileProcessor(Statement stmt) {
        this.stmt = stmt;
        this.inspector = new SchemaInspector();
    }

    void process(@NotNull Path file) throws SQLException {
        String path = file.toAbsolutePath().toString().replace("\\", "\\\\");
        stmt.execute(String.format(
                "CREATE TABLE temp_trips AS SELECT * FROM read_parquet('%s')", path
        ));

        try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(temp_trips)")) {
            ColumnInfo info = inspector.inspect(rs);
            if (info.hasTaxiType()) {
                stmt.execute(SQLBuilder.buildInsertSql(info));
            }
        } finally {
            stmt.execute("DROP TABLE IF EXISTS temp_trips");
        }
    }
}

class SchemaInspector {
    ColumnInfo inspect(@NotNull ResultSet rs) throws SQLException {
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
        return new ColumnInfo(pickup, dropoff, type);
    }
}

record ColumnInfo(String pickupColumn, String dropoffColumn, String taxiType) {
    boolean hasTaxiType() {
        return taxiType != null;
    }
}

class SQLBuilder {
    @Contract(pure = true)
    static @NotNull String createTripsTable() {
        return "CREATE TABLE IF NOT EXISTS trips (" +
                "vendor_id INTEGER, pickup_datetime TIMESTAMP, dropoff_datetime TIMESTAMP, " +
                "passenger_count INTEGER, trip_distance DOUBLE, rate_code_id INTEGER, " +
                "pu_location_id INTEGER, do_location_id INTEGER, payment_type INTEGER, " +
                "fare_amount DOUBLE, extra DOUBLE, mta_tax DOUBLE, tip_amount DOUBLE, " +
                "tolls_amount DOUBLE, improvement_surcharge DOUBLE, total_amount DOUBLE, " +
                "congestion_surcharge DOUBLE, taxi_type VARCHAR" +
                ")";
    }

    static @NotNull String buildInsertSql(@NotNull ColumnInfo info) {
        return String.format(
                "INSERT INTO trips SELECT VendorID as vendor_id, %s AS pickup_datetime, %s AS dropoff_datetime, " +
                        "RatecodeID as rate_code_id, PULocationID as pu_location_id, DOLocationID as do_location_id, " +
                        "passenger_count, trip_distance, payment_type, fare_amount, extra, mta_tax, tip_amount, " +
                        "tolls_amount, improvement_surcharge, total_amount, congestion_surcharge, '%s' AS taxi_type " +
                        "FROM temp_trips",
                info.pickupColumn(), info.dropoffColumn(), info.taxiType()
        );
    }
}
package com.xiaodi.taxi.etl;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.util.stream.Stream;

/**
 * ETL class to insert all parquets in parquets folder into the duckdb in duck-db/nyc_taxi_combined.duckdb
 */
public class ParquetsDBInserter {
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

    private final DirectoryScanner directoryScanner;
    private final ConnectionFactory ConnectionFactory;

    /**
     * Constructor
     * @param directoryScanner An instance of a directory scanner.
     * @param ConnectionFactory An instance of a connection factory.
     */
    public ParquetsDBInserter(DirectoryScanner directoryScanner, ConnectionFactory ConnectionFactory) {
        this.directoryScanner = directoryScanner;
        this.ConnectionFactory = ConnectionFactory;
    }

    /**
     * executes the ETL job.
     * @param inputDir parquets directory
     * @param outputFile duckdb output file
     */
    public void run(Path inputDir, Path outputFile) throws IOException, SQLException {
        ensureOutputDirectoryExists(outputFile);
        deleteExistingFile(outputFile);

        try (Connection conn = ConnectionFactory.getConnection("jdbc:duckdb:" + outputFile.toString());
             Statement stmt = conn.createStatement()) {

            stmt.execute(SQLBuilder.createTripsTable());

            try (Stream<Path> files = directoryScanner.listParquetFiles(inputDir)) {
                files.forEach(path -> {
                    try {
                        SQLExecutor processor = new SQLExecutor(stmt);
                        processor.execute(path);
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

record ColumnInfo(String pickupColumn, String dropoffColumn, String taxiType) {
    boolean hasTaxiType() {
        return taxiType != null;
    }
}


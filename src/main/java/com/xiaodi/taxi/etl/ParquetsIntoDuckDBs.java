package com.xiaodi.taxi.etl;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.util.stream.Stream;

public class ParquetsIntoDuckDBs {

    public static void main(String[] args) {
        String inputDir = "parquets";
        String outputDir = "duck-dbs";

        try {
            // Ensure output directory exists
            Path outputPath = Paths.get(outputDir);
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
            }

            try (Stream<Path> files = Files.list(Paths.get(inputDir))) {
                for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".parquet"))::iterator) {
                    String inputPath = file.toAbsolutePath().toString().replace("\\", "\\\\");

                    String baseName = file.getFileName().toString().replaceFirst("\\.parquet$", "");
                    String dbPath = outputDir + File.separator + baseName + ".duckdb";

                    File dbFile = new File(dbPath);
                    if (dbFile.exists() && !dbFile.delete()) {
                        throw new IOException("Unable to delete existing DB file: " + dbPath);
                    }

                    try (Connection conn = DriverManager.getConnection("jdbc:duckdb:" + dbPath)) {
                        Statement stmt = conn.createStatement();

                        // Create a table directly from the parquet file
                        stmt.execute(String.format("""
                            CREATE TABLE trips AS
                            SELECT * FROM read_parquet('%s')
                        """, inputPath));

                        System.out.println("✅ Created: " + dbPath);
                    }
                }
            }

            System.out.println("🎉 ETL split complete. One DB per file written to: " + outputDir);

        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }
}

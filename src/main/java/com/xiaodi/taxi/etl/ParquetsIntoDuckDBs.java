package com.xiaodi.taxi.etl;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.util.stream.Stream;

public class ParquetsIntoDuckDBs {

    public static void main(String[] args) {
        String inputDir = "parquets";
        String outputDir = "duck-db/nyc_taxi_combined.duckdb";

        try {
            // Ensure output directory exists
            Path outputPath = Paths.get(outputDir);
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
            }

            File dbFile = new File(outputDir);
            if (dbFile.exists() && !dbFile.delete()) {
                throw new IOException("Unable to delete existing DB file: " + outputDir);
            }

            try (Connection conn = DriverManager.getConnection("jdbc:duckdb:" + outputDir)) {
                Statement stmt = conn.createStatement();

                System.out.println("✅ Created: " + outputDir);

                // Create the trips table if it doesn't exist
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS trips (
                        vendorid INTEGER,
                        pickup_datetime TIMESTAMP,
                        dropoff_datetime TIMESTAMP,
                        passenger_count INTEGER,
                        trip_distance DOUBLE,
                        rate_code_id INTEGER,
                        pu_location_id INTEGER,
                        do_location_id INTEGER,
                        payment_type INTEGER,
                        fare_amount DOUBLE,
                        extra DOUBLE,
                        mta_tax DOUBLE,
                        tip_amount DOUBLE,
                        tolls_amount DOUBLE,
                        improvement_surcharge DOUBLE,
                        total_amount DOUBLE,
                        congestion_surcharge DOUBLE,
                        taxi_type VARCHAR
                    )
                """);

                try (Stream<Path> files = Files.list(Paths.get(inputDir))) {
                    files.filter(p -> p.toString().endsWith(".parquet"))
                            .forEach(file -> {
                                try {
                                    String inputPath = file.toAbsolutePath().toString().replace("\\", "\\\\");

                                    // First, load Parquet file into a temporary table to inspect the columns
                                    stmt.execute(String.format("CREATE TABLE temp_trips AS SELECT * FROM read_parquet('%s')", inputPath));

                                    // Check the columns of the temp table to determine which columns exist
                                    ResultSet rs = stmt.executeQuery("PRAGMA table_info(temp_trips);");
                                    String pickupColumn = null;
                                    String dropoffColumn = null;
                                    String taxiType = null;

                                    // Look for the pickup and dropoff columns
                                    while (rs.next()) {
                                        String columnName = rs.getString("name");

                                        if ("tpep_pickup_datetime".equals(columnName)) {
                                            taxiType = "yellow";  // Assign taxiType if tpep is found
                                        }
                                        if ("lpep_pickup_datetime".equals(columnName)) {
                                            taxiType = "green";  // Assign taxiType if lpep is found
                                        }

                                        // Check and assign pickup and dropoff columns
                                        if ("lpep_pickup_datetime".equals(columnName) || "tpep_pickup_datetime".equals(columnName)) {
                                            pickupColumn = columnName;
                                        }
                                        if ("lpep_dropoff_datetime".equals(columnName) || "tpep_dropoff_datetime".equals(columnName)) {
                                            dropoffColumn = columnName;
                                        }
                                    }

                                    // Normalize columns and insert into the main table
                                    if (taxiType != null) {
                                        stmt.execute(String.format("""
                                         INSERT INTO trips
                                         SELECT
                                             VendorID as vendorid,
                                             %s AS pickup_datetime,
                                             %s AS dropoff_datetime,
                                             RatecodeID as rate_code_id,
                                             PULocationID as pu_location_id,
                                             DOLocationID as do_location_id,
                                             passenger_count,
                                             trip_distance,
                                             payment_type,
                                             fare_amount,
                                             extra,
                                             mta_tax,
                                             tip_amount,
                                             tolls_amount,
                                             improvement_surcharge,
                                             total_amount,
                                             congestion_surcharge,
                                             '%s' AS taxi_type
                                         FROM temp_trips
                                     """, pickupColumn, dropoffColumn, taxiType));
                                    }

                                    // Clean up by dropping the temp table after use
                                    stmt.execute("DROP TABLE temp_trips");

                                } catch (SQLException e) {
                                    e.printStackTrace(); // Handle exceptions during each file processing
                                }
                            });

                    System.out.println("🎉 ETL split complete. One DB per file written to: " + outputDir);
                }
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }
}

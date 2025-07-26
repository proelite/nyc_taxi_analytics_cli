package com.xiaodi.taxi.etl;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.sql.*;

public class MergeDuckDBs {

    public static void main(String[] args) throws IOException {
        String inputDir = "duck-dbs";
        String combinedDbPath = "combined-duck-db/nyc_taxi_combined.duckdb";

        try {
            // Ensure output dir exists
            Path outputPath = Paths.get(combinedDbPath);
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
            }

            File dbFile = new File(combinedDbPath);
            if (dbFile.exists() && !dbFile.delete()) {
                throw new IOException("Unable to delete existing DB file: " + combinedDbPath);
            }

            try (Connection conn = DriverManager.getConnection("jdbc:duckdb:" + combinedDbPath)) {
                Statement stmt = conn.createStatement();

                // Create final normalized table
                stmt.execute("""
                            CREATE TABLE trips (
                                vendorid INTEGER,
                                pickup_datetime TIMESTAMP,
                                dropoff_datetime TIMESTAMP,
                                passenger_count INTEGER,
                                trip_distance DOUBLE,
                                ratecodeid INTEGER,
                                store_and_fwd_flag TEXT,
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
                                ehail_fee DOUBLE,
                                trip_type INTEGER,
                                airport_fee DOUBLE,
                                taxi_type TEXT
                            )
                        """
                );

                // Iterate through all .duckdb files
                for (Path file : (Iterable<Path>) Files.list(Paths.get(inputDir)).filter(p -> p.toString().endsWith(".duckdb"))::iterator) {
                    String dbPath = file.toAbsolutePath().toString().replace("\\", "\\\\");

                    // Attach external DuckDB file
                    String attachName = "src" + Math.abs(file.getFileName().toString().hashCode());
                    System.out.println("attachName: " + attachName);

                    stmt.execute("ATTACH DATABASE '" + dbPath + "' AS " + attachName);

                    // Insert normalized data into final table
                    stmt.execute(String.format("""
                                    INSERT INTO trips
                                    SELECT
                                    vendorid,
                                    COALESCE(tpep_pickup_datetime, lpep_pickup_datetime) AS pickup_datetime,
                                    COALESCE(tpep_dropoff_datetime, lpep_dropoff_datetime) AS dropoff_datetime,
                                    passenger_count,
                                    trip_distance,
                                    ratecodeid,
                                    store_and_fwd_flag,
                                    pu_location_id,
                                    do_location_id,
                                    payment_type,
                                    fare_amount,
                                    extra,
                                    mta_tax,
                                    tip_amount,
                                    tolls_amount,
                                    improvement_surcharge,
                                    total_amount,
                                    congestion_surcharge,
                                    ehail_fee,
                                    trip_type,
                                    airport_fee,
                                    CASE
                                        WHEN tpep_pickup_datetime IS NOT NULL THEN 'yellow'
                                        WHEN lpep_pickup_datetime IS NOT NULL THEN 'green'
                                        ELSE 'unknown'
                                    END AS taxi_type
                            FROM %s.trips
                            WHERE COALESCE(tpep_pickup_datetime, lpep_pickup_datetime) IS NOT NULL
                            """, attachName));

                    stmt.execute("DETACH DATABASE " + attachName);
                    System.out.println("✅ Merged from: " + dbPath);
                }

                System.out.println("🎉 Combined DuckDB complete at: " + combinedDbPath);
            }

        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }
}

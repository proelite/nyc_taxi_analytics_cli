package com.xiaodi.taxi.etl;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class SQLBuilder {
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

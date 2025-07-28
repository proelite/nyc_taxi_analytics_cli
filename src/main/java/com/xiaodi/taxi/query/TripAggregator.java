package com.xiaodi.taxi.query;

import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.List;

/**
 * This class is entry point to execute a query for trip aggregates using the main method.
 */
public class TripAggregator {
    public static final String ANSI_BOLD  = "\u001B[1m";
    public static final String ANSI_RESET = "\u001B[0m";

    public static void main(String @NotNull [] args) throws Exception {
        if (args.length != 7) {
            System.err.println("Usage: startTime dropoffDatetime puLocationID doLocationID groupByPayment vendorID taxiType");
            return;
        }
        TripQueryParams params = TripQueryParams.builder()
                .pickupDatetime(args[0])
                .dropoffDatetime(args[1])
                .puLocationID(args[2])
                .doLocationID(args[3])
                .groupByPayment(Boolean.parseBoolean(args[4]))
                .vendorID(args[5])
                .taxiType(args[6])
                .build();

        String dbPath = "duck-db/nyc_taxi_combined.duckdb";
        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:" + dbPath)) {
            var service = new TripAggregatorService(conn);
            List<TripAggregationResult> rows = service.aggregate(params);
            rows.forEach(row -> {
                System.out.println(ANSI_BOLD + "Taxi Type: " + ANSI_RESET + row.taxiType());
                System.out.println(ANSI_BOLD + "Vendor: " + ANSI_RESET + row.vendor());
                System.out.println(ANSI_BOLD + "Payment Type: " + ANSI_RESET + row.paymentType());
                System.out.println(ANSI_BOLD + "Min Fare: " + ANSI_RESET + row.minFare());
                System.out.println(ANSI_BOLD + "Max Fare: " + ANSI_RESET + row.maxFare());
                System.out.println(ANSI_BOLD + "Count of Trips: " + ANSI_RESET + row.tripCount());
                System.out.println(ANSI_BOLD + "Total Toll Fare Sum: " + ANSI_RESET + row.totalTollFare());
                System.out.println(ANSI_BOLD + "Total Fare Sum: " + ANSI_RESET + row.totalFare());
                System.out.println("-------");
            });
        }
    }
}

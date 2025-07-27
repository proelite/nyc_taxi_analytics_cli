package com.xiaodi.taxi.query;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TripAggregator {
    public static String EMPTY_VALUE = "*";
    public static Map<String, String> VENDOR_MAP = Map.of(
            "1", "Creative Mobile Technologies, LLC",
            "2", "Curb Mobility, LLC",
             "6", "Myle Technologies Inc",
            "7", "Helix"
    );

    private static final Map<String, String> PAYMENT_MAP = Map.of(
            "0", "Flex Fare trip",
            "1", "Credit card",
            "2", "Cash",
            "3", "No charge",
            "4", "Dispute",
            "5", "Unknown",
            "6", "Voided trip"
    );
    public static final String ANSI_BOLD  = "\u001B[1m";
    public static final String ANSI_RESET = "\u001B[0m";

    public static void main(String[] args) {
        // Arguments for query: [pickupDatetime, dropoffDatetime, puLocationID, doLocationID, groupByPayment, vendorID, taxiType]
        if (args.length != 7) {
            System.err.println("Please provide the required arguments: startTime dropoff_datetime pu_location_id do_location_id groupByPayment vendorId  taxiType");
            return;
        }

        String pickupDatetime = args[0];  // Timestamp (start time for trips)
        String dropoffDatetime = args[1];    // Timestamp (end time for trips)
        String puLocationID = args[2]; // Start location id
        String doLocationID = args[3];   // End location id
        boolean groupByPayment = Boolean.parseBoolean(args[4]);
        String vendorID = args[5];         // Vendor ID
        String taxiType = args[6];                         // Taxi type (yellow, green)

        String dbPath = "duck-db/nyc_taxi_combined.duckdb";  // Path to DuckDB file

        // Initialize the connection to DuckDB
        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:" + dbPath)) {
            String query = buildQuery(pickupDatetime, dropoffDatetime, puLocationID, doLocationID, groupByPayment, vendorID, taxiType);
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                setQueryParameters(stmt, pickupDatetime, dropoffDatetime, puLocationID, doLocationID, vendorID, taxiType);
                ResultSet rs = stmt.executeQuery();

                System.out.println("Results");
                System.out.println("---------------------------------------------------");

                while (rs.next()) {
                    if (taxiType != null && !EMPTY_VALUE.equals(taxiType)) {
                        System.out.println(ANSI_BOLD + "Taxi Type: " + ANSI_RESET + taxiType);
                    } else {
                        System.out.println(ANSI_BOLD + "Taxi Type: " + ANSI_RESET + "yellow and green");
                    }
                    if (vendorID != null && !EMPTY_VALUE.equals(vendorID)) {
                        System.out.println(ANSI_BOLD + "Vendor: " + ANSI_RESET + VENDOR_MAP.get(vendorID));
                    } else {
                        System.out.println(ANSI_BOLD + "Vendor: " + ANSI_RESET + "all");
                    }
                    if (groupByPayment && rs.getObject("payment_type") != null) {
                        System.out.println(ANSI_BOLD + "Payment Type: " + PAYMENT_MAP.get(rs.getString("payment_type")));
                    } else {
                        System.out.println(ANSI_BOLD + "Payment Type: " + ANSI_RESET + "all");
                    }
                    System.out.println(ANSI_BOLD + "Min Fare: " + ANSI_RESET + rs.getDouble("min_fare"));
                    System.out.println(ANSI_BOLD + "Max Fare: " + ANSI_RESET + rs.getDouble("max_fare"));
                    System.out.println(ANSI_BOLD + "Count of Trips: " + ANSI_RESET + rs.getInt("trip_count"));
                    System.out.println(ANSI_BOLD + "Total Toll Fare Sum: " + ANSI_RESET + rs.getDouble("total_toll_fare"));
                    System.out.println(ANSI_BOLD + "Total Fare Sum: " + ANSI_RESET + rs.getDouble("total_fare"));
                    System.out.println("-------");
                }

                System.out.println("---------------------------------------------------");
                rs.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static String buildQuery(String pickupDatetime, String dropoffDatetime, String puLocationID, String doLocationID, boolean groupByPayment, String vendorID, String taxiType) {
        StringBuilder query = new StringBuilder("SELECT ");
        query.append("MIN(fare_amount) AS min_fare, ");
        query.append("MAX(fare_amount) AS max_fare, ");
        query.append("COUNT(*) AS trip_count, ");
        query.append("SUM(fare_amount) AS total_fare, ");
        query.append("SUM(tolls_amount) AS total_toll_fare, ");

        if (groupByPayment) {
            query.append("payment_type, ");
        }

        query.append("FROM trips ");

        List<String> filters = new ArrayList<>();

        // Apply filters when needed
        if (pickupDatetime != null && !EMPTY_VALUE.equals(pickupDatetime)) {
            filters.add("pickup_datetime >= ?");
        }

        if (dropoffDatetime != null && !EMPTY_VALUE.equals(dropoffDatetime)) {
            filters.add("dropoff_datetime <= ?");
        }

        if (puLocationID != null && !EMPTY_VALUE.equals(puLocationID)) {
            filters.add("pu_location_id = ?");
        }

        if (doLocationID != null && !EMPTY_VALUE.equals(doLocationID)) {
            filters.add("do_location_id = ?");
        }

        if (vendorID != null && !EMPTY_VALUE.equals(vendorID)) {
            filters.add("vendor_id = ?");
        }

        if (taxiType != null && !EMPTY_VALUE.equals(taxiType)) {
            if ("yellow".equalsIgnoreCase(taxiType) || "green".equalsIgnoreCase(taxiType)) {
                filters.add("taxi_type = ? ");
            }
            // "both" case - no additional filter needed
        }

        if (!filters.isEmpty()) {
            query.append("WHERE ");
            query.append(String.join(" AND ", filters));
        }

        if (groupByPayment) {
            query.append("GROUP BY payment_type");
        }

        return query.toString();
    }

    // Helper method to set parameters in PreparedStatement
    private static void setQueryParameters(PreparedStatement pstmt, String pickupDatetime, String dropoffDatetime, String puLocationID, String doLocationID, String vendorID, String taxiType) throws SQLException {
        // Set required parameters
        int index = 1;

        if (pickupDatetime != null && !EMPTY_VALUE.equals(pickupDatetime) ) {
            pstmt.setString(index++, pickupDatetime);
        }

        if (dropoffDatetime != null && !EMPTY_VALUE.equals(dropoffDatetime)) {
            pstmt.setString(index++, dropoffDatetime);
        }

        if (puLocationID != null && !EMPTY_VALUE.equals(puLocationID)) {
            pstmt.setInt(index++, Integer.parseInt(puLocationID));
        }

        if (doLocationID != null && !EMPTY_VALUE.equals(doLocationID)) {
            pstmt.setInt(index++, Integer.parseInt(doLocationID));
        }

        if (vendorID != null && !EMPTY_VALUE.equals(vendorID)) {
            pstmt.setInt(index++, Integer.parseInt(vendorID));
        }

        if (taxiType != null && !taxiType.isEmpty()) {
            if ("yellow".equalsIgnoreCase(taxiType) || "green".equalsIgnoreCase(taxiType)) {
                pstmt.setString(index, taxiType.toLowerCase());
            }
        }
    }
}

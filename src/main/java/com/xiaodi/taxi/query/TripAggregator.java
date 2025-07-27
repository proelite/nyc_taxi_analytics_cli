package com.xiaodi.taxi.query;

import java.sql.*;
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

    public static void main(String[] args) {
        // Arguments for query: [pickupDatetime, dropoffDatetime, puLocationID, doLocationID, groupByPayment, vendorID, taxiType]
        if (args.length != 7) {
            System.out.println("Please provide the required arguments: startTime dropoff_datetime pu_location_id do_location_id groupByPayment vendorId  taxiType");
            return;
        }

        String pickupDatetime = args[0];  // Timestamp (start time for trips)
        String dropoffDatetime = args[1];    // Timestamp (end time for trips)
        int puLocationID = Integer.parseInt(args[2]); // Start location id
        int doLocationID = Integer.parseInt(args[3]);   // End location id
        boolean groupByPayment = Boolean.parseBoolean(args[4]);
        String vendorID = args[5];         // Vendor ID
        String taxiType = args[6];                         // Taxi type (yellow, green)

        String dbPath = "duck-db/nyc_taxi_combined.duckdb";  // Path to DuckDB file

        // Initialize the connection to DuckDB
        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:" + dbPath)) {
            String query = buildQuery(groupByPayment, vendorID, taxiType);
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                setQueryParameters(stmt, pickupDatetime, dropoffDatetime, puLocationID, doLocationID, vendorID, taxiType);
                ResultSet rs = stmt.executeQuery();

                System.out.println("Results");
                System.out.println("---------------------------------------------------");

                while (rs.next()) {
                    if (taxiType != null && !EMPTY_VALUE.equals(taxiType)) {
                        System.out.println("Taxi Type: " + taxiType);
                    } else {
                        System.out.println("Taxi Type: yellow and green");
                    }
                    if (vendorID != null && !EMPTY_VALUE.equals(vendorID)) {
                        System.out.println("Vendor: " + VENDOR_MAP.get(vendorID));
                    } else {
                        System.out.println("Vendor: all");
                    }
                    if (groupByPayment) {
                        System.out.println("Payment Type: " + PAYMENT_MAP.get(rs.getString("payment_type")));
                    } else {
                        System.out.println("Payment Type: all");
                    }
                    System.out.println("Min Fare: " + rs.getDouble("min_fare"));
                    System.out.println("Max Fare: " + rs.getDouble("max_fare"));
                    System.out.println("Count of Trips: " + rs.getInt("trip_count"));
                    System.out.println("Total Toll Fare Sum: " + rs.getDouble("total_toll_fare"));
                    System.out.println("Total Fare Sum: " + rs.getDouble("total_fare"));
                    System.out.println("-------");
                }

                System.out.println("---------------------------------------------------");
                rs.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static String buildQuery(boolean groupByPayment, String vendorID, String taxiType) {
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

        // Apply filters (all required parameters)
        query.append("WHERE pickup_datetime >= ? AND dropoff_datetime <= ? ");
        query.append("AND pu_location_id = ? ");
        query.append("AND do_location_id = ? ");
        if (vendorID != null && !EMPTY_VALUE.equals(vendorID)) {
            query.append("AND vendor_id = ? ");
        }

        // Add taxi type filter only when needed
        if (taxiType != null && !EMPTY_VALUE.equals(taxiType)) {
            if ("yellow".equalsIgnoreCase(taxiType) || "green".equalsIgnoreCase(taxiType)) {
                query.append("AND taxi_type = ? ");
            }
            // "both" case - no additional filter needed
        }

        if (groupByPayment) {
            query.append("GROUP BY payment_type");
        }

        return query.toString();
    }

    // Helper method to set parameters in PreparedStatement
    private static void setQueryParameters(PreparedStatement pstmt, String pickupDatetime, String dropoffDatetime,
                                           int puLocationID, int doLocationID, String vendorID, String taxiType) throws SQLException {
        // Set required parameters
        int index = 1;
        pstmt.setString(index++, pickupDatetime);
        pstmt.setString(index++, dropoffDatetime);
        pstmt.setInt(index++, puLocationID);
        pstmt.setInt(index++, doLocationID);
        if (vendorID != null && !EMPTY_VALUE.equals(vendorID)) {
            pstmt.setInt(index++, Integer.parseInt(vendorID));
        }

        // Set optional taxi type parameter
        if (taxiType != null && !taxiType.isEmpty()) {
            if ("yellow".equalsIgnoreCase(taxiType) || "green".equalsIgnoreCase(taxiType)) {
                pstmt.setString(index, taxiType.toLowerCase());
            }
        }
    }
}

package com.xiaodi.taxi.query;

import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.*;


/**
 * Service class encapsulating query construction, parameter binding, and result mapping.
 */
public class TripAggregatorService {
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

    private final Connection connection;

    TripAggregatorService(Connection conn) {
        this.connection = conn;
    }

    public List<TripAggregationResult> aggregate(TripQueryParams params) throws SQLException {
        String sql = buildQuery(params);
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            bindParameters(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                return mapResults(rs, params);
            }
        }
    }

    String buildQuery(@NotNull TripQueryParams p) {
        StringBuilder q = new StringBuilder("SELECT MIN(fare_amount) AS min_fare, MAX(fare_amount) AS max_fare, COUNT(*) AS trip_count, SUM(fare_amount) AS total_fare, SUM(tolls_amount) AS total_toll_fare");
        if (p.isGroupByPayment()) {
            q.append(", payment_type");
        }
        q.append(" FROM trips");

        var filters = getStrings(p);
        if (!filters.isEmpty()) {
            q.append(" WHERE ")
                    .append(String.join(" AND ", filters));
        }
        if (p.isGroupByPayment()) {
            q.append(" GROUP BY payment_type");
        }
        return q.toString();
    }

    private static @NotNull ArrayList<String> getStrings(@NotNull TripQueryParams p) {
        var filters = new ArrayList<String>();
        if (!TripQueryParams.EMPTY_VALUE.equals(p.getPickupDatetime())) filters.add("pickup_datetime >= ?");
        if (!TripQueryParams.EMPTY_VALUE.equals(p.getDropoffDatetime())) filters.add("dropoff_datetime <= ?");
        if (!TripQueryParams.EMPTY_VALUE.equals(p.getPuLocationID())) filters.add("pu_location_id = ?");
        if (!TripQueryParams.EMPTY_VALUE.equals(p.getDoLocationID())) filters.add("do_location_id = ?");
        if (!TripQueryParams.EMPTY_VALUE.equals(p.getVendorID())) filters.add("vendor_id = ?");
        if (!TripQueryParams.EMPTY_VALUE.equals(p.getTaxiType())) {
            if ("yellow".equalsIgnoreCase(p.getTaxiType()) || "green".equalsIgnoreCase(p.getTaxiType())) {
                filters.add("taxi_type = ?");
            }
        }
        return filters;
    }

    void bindParameters(PreparedStatement stmt, @NotNull TripQueryParams p) throws SQLException {
        int idx = 1;
        if (!TripQueryParams.EMPTY_VALUE.equals(p.getPickupDatetime())) stmt.setString(idx++, p.getPickupDatetime());
        if (!TripQueryParams.EMPTY_VALUE.equals(p.getDropoffDatetime())) stmt.setString(idx++, p.getDropoffDatetime());
        if (!TripQueryParams.EMPTY_VALUE.equals(p.getPuLocationID())) stmt.setInt(idx++, Integer.parseInt(p.getPuLocationID()));
        if (!TripQueryParams.EMPTY_VALUE.equals(p.getDoLocationID())) stmt.setInt(idx++, Integer.parseInt(p.getDoLocationID()));
        if (!TripQueryParams.EMPTY_VALUE.equals(p.getVendorID())) stmt.setInt(idx++, Integer.parseInt(p.getVendorID()));
        if (!TripQueryParams.EMPTY_VALUE.equals(p.getTaxiType())) {
            if ("yellow".equalsIgnoreCase(p.getTaxiType()) || "green".equalsIgnoreCase(p.getTaxiType())) {
                stmt.setString(idx, p.getTaxiType().toLowerCase());
            }
        }
    }

    List<TripAggregationResult> mapResults(@NotNull ResultSet rs, TripQueryParams p) throws SQLException {
        List<TripAggregationResult> results = new ArrayList<>();
        while (rs.next()) {
            String taxiType = TripQueryParams.EMPTY_VALUE.equals(p.getTaxiType())
                    ? "yellow and green"
                    : p.getTaxiType();

            String vendor = TripQueryParams.EMPTY_VALUE.equals(p.getVendorID())
                    ? "all"
                    : VENDOR_MAP.get(p.getVendorID());

            String payment = p.isGroupByPayment() && rs.getObject("payment_type") != null
                    ? PAYMENT_MAP.getOrDefault(rs.getString("payment_type"), "all")
                    : "all";

            results.add(new TripAggregationResult(
                    taxiType,
                    vendor,
                    payment,
                    rs.getDouble("min_fare"),
                    rs.getDouble("max_fare"),
                    rs.getInt("trip_count"),
                    rs.getDouble("total_toll_fare"),
                    rs.getDouble("total_fare")
            ));
        }
        return results;
    }
}
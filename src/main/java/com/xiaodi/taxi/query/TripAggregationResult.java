package com.xiaodi.taxi.query;

/**
 * Represents an aggregated result row.
 */
public record TripAggregationResult(String taxiType, String vendor, String paymentType, double minFare, double maxFare,
                                    int tripCount, double totalTollFare, double totalFare) {
}

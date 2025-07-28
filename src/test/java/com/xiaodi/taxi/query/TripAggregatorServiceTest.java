package com.xiaodi.taxi.query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TripAggregatorServiceTest {
    private TripAggregatorService service;

    @BeforeEach
    void setUp() {
        // Connection isn't used by buildQuery/bindParameters/mapResults
        service = new TripAggregatorService(mock(Connection.class));
    }

    @Test
    void testBuildQuery_noFilters_noGroup() {
        TripQueryParams params = TripQueryParams.builder().build();
        String expected = "SELECT MIN(fare_amount) AS min_fare, MAX(fare_amount) AS max_fare, COUNT(*) AS trip_count, SUM(fare_amount) AS total_fare, SUM(tolls_amount) AS total_toll_fare FROM trips";
        String actual = service.buildQuery(params);
        assertEquals(expected, actual);
    }

    @Test
    void testBuildQuery_withFiltersAndGroup() {
        TripQueryParams params = TripQueryParams.builder()
                .pickupDatetime("2025-01-01")
                .dropoffDatetime("2025-01-31")
                .puLocationID("5")
                .doLocationID("10")
                .vendorID("2")
                .taxiType("yellow")
                .groupByPayment(true)
                .build();
        String expected =
                "SELECT MIN(fare_amount) AS min_fare, MAX(fare_amount) AS max_fare, COUNT(*) AS trip_count, SUM(fare_amount) AS total_fare, SUM(tolls_amount) AS total_toll_fare, payment_type " +
                        "FROM trips " +
                        "WHERE pickup_datetime >= ? AND dropoff_datetime <= ? AND pu_location_id = ? AND do_location_id = ? AND vendor_id = ? AND taxi_type = ? " +
                        "GROUP BY payment_type";
        String actual = service.buildQuery(params);
        assertEquals(expected, actual);
    }

    @Test
    void testBindParameters() throws SQLException {
        PreparedStatement pstmt = mock(PreparedStatement.class);
        TripQueryParams params = TripQueryParams.builder()
                .pickupDatetime("2025-01-01")
                .dropoffDatetime("2025-01-31")
                .puLocationID("5")
                .doLocationID("10")
                .vendorID("2")
                .taxiType("green")
                .build();
        service.bindParameters(pstmt, params);

        verify(pstmt).setString(1, "2025-01-01");
        verify(pstmt).setString(2, "2025-01-31");
        verify(pstmt).setInt(3, 5);
        verify(pstmt).setInt(4, 10);
        verify(pstmt).setInt(5, 2);
        verify(pstmt).setString(6, "green");
        verifyNoMoreInteractions(pstmt);
    }

    @Test
    void testMapResults_groupByPayment() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(true, true, false);
        when(rs.getDouble("min_fare")).thenReturn(2.0, 3.0);
        when(rs.getDouble("max_fare")).thenReturn(5.0, 6.0);
        when(rs.getInt("trip_count")).thenReturn(10, 20);
        when(rs.getDouble("total_toll_fare")).thenReturn(1.5, 2.5);
        when(rs.getDouble("total_fare")).thenReturn(20.0, 30.0);

        TripQueryParams params = TripQueryParams.builder()
                .groupByPayment(true)
                .build();
        List<TripAggregationResult> results = service.mapResults(rs, params);

        assertEquals(2, results.size());

        TripAggregationResult first = results.get(0);
        assertEquals("yellow and green", first.taxiType());
        assertEquals("all", first.vendor());
        assertEquals("all", first.paymentType());
        assertEquals(2.0, first.minFare());
        assertEquals(5.0, first.maxFare());
        assertEquals(10, first.tripCount());
        assertEquals(1.5, first.totalTollFare());
        assertEquals(20.0, first.totalFare());

        TripAggregationResult second = results.get(1);
        assertEquals("all", second.paymentType());
        assertEquals(3.0, second.minFare());
        assertEquals(6.0, second.maxFare());
        assertEquals(20, second.tripCount());
        assertEquals(2.5, second.totalTollFare());
        assertEquals(30.0, second.totalFare());
    }

    @Test
    void testTripQueryParamsBuilderDefaults() {
        TripQueryParams params = TripQueryParams.builder().build();
        assertEquals(TripQueryParams.EMPTY_VALUE, params.getPickupDatetime());
        assertEquals(TripQueryParams.EMPTY_VALUE, params.getDropoffDatetime());
        assertEquals(TripQueryParams.EMPTY_VALUE, params.getPuLocationID());
        assertEquals(TripQueryParams.EMPTY_VALUE, params.getDoLocationID());
        assertFalse(params.isGroupByPayment());
        assertEquals(TripQueryParams.EMPTY_VALUE, params.getVendorID());
        assertEquals(TripQueryParams.EMPTY_VALUE, params.getTaxiType());
    }
}
import com.xiaodi.taxi.query.models.TripAggregationResult;
import com.xiaodi.taxi.query.TripAggregatorService;
import com.xiaodi.taxi.query.models.TripQueryParams;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for TripAggregatorService against the real DuckDB file.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TripAggregatorServiceIntegrationTest {
    private static final String DB_FILE_PATH = "duck-db/nyc_taxi_combined.duckdb";
    private Connection connection;

    @BeforeAll
    void setup() throws Exception {
        // Ensure the DuckDB file exists
        File dbFile = new File(DB_FILE_PATH);
        assertTrue(dbFile.exists(),
                "Database file '" + DB_FILE_PATH + "' not found. " +
                        "Please run the Gradle task 'downloadInsertParquetsIntoDBs'.");

        // Open a real connection to DuckDB
        String jdbcUrl = "jdbc:duckdb:" + DB_FILE_PATH;
        connection = DriverManager.getConnection(jdbcUrl);
        assertNotNull(connection, "Failed to open DuckDB connection");
    }

    @Test
    void wildcardNoGroupBy_returnsSingleAggregate() throws Exception {
        TripQueryParams params = TripQueryParams.builder()
                .pickupDatetime("*")
                .dropoffDatetime("*")
                .puLocationID("*")
                .doLocationID("*")
                .groupByPayment(false)
                .vendorID("*")
                .taxiType("*")
                .build();

        TripAggregatorService service = new TripAggregatorService(connection);
        List<TripAggregationResult> results = service.aggregate(params);

        assertEquals(1, results.size(), "Expected at least one row for all wild cards");
        TripAggregationResult row = results.getFirst();
        assertEquals("all", row.vendor(), "Expect all for vendor");
        assertEquals("yellow and green", row.taxiType(), "Expect yellow and green for taxi type");
        assertEquals("all", row.paymentType());
        assertEquals(9019594, row.tripCount());
        assertEquals(-998.0, row.minFare());
        assertEquals(325478.05, row.maxFare());
        assertEquals(168306161, Math.round(row.totalFare()));
        assertEquals(4581481,  Math.round(row.totalTollFare()));
    }

    @Test
    void wildcardGroupBy_returnsMultiplePaymentRows() throws Exception {
        TripQueryParams params = TripQueryParams.builder()
                .pickupDatetime("*")
                .dropoffDatetime("*")
                .puLocationID("*")
                .doLocationID("*")
                .groupByPayment(true)
                .vendorID("*")
                .taxiType("*")
                .build();

        TripAggregatorService service = new TripAggregatorService(connection);
        List<TripAggregationResult> results = service.aggregate(params);

        // With grouping, expect multiple rows (one per payment type present)
        assertEquals(7, results.size(), "Expect all groups due to all seven payment types");
        var paymentSet = new HashSet<>(TripAggregatorService.PAYMENT_MAP.values());
        for (var result: results) {
            paymentSet.remove(result.paymentType());
        }
        assertEquals(1, paymentSet.size());
        assertTrue(paymentSet.contains("Voided trip"));
    }

    @Test
    void specificVendorFilter_noGroupBy_returnsVendorOnly() throws Exception {
        String testVendor = "1"; // Creative Mobile Technologies, LLC
        TripQueryParams params = TripQueryParams.builder()
                .pickupDatetime("*")
                .dropoffDatetime("*")
                .puLocationID("*")
                .doLocationID("*")
                .groupByPayment(false)
                .vendorID(testVendor)
                .taxiType("*")
                .build();

        TripAggregatorService service = new TripAggregatorService(connection);
        List<TripAggregationResult> results = service.aggregate(params);

        assertEquals(1, results.size());
        TripAggregationResult row = results.getFirst();
        assertEquals(TripAggregatorService.VENDOR_MAP.get(testVendor), row.vendor());
    }

    @Test
    void specificTaxiTypeAndVendor_groupByPayment_returnsFilteredRows() throws Exception {
        String testVendor = "2"; // Curb Mobility, LLC
        String testTaxiType = "green";
        TripQueryParams params = TripQueryParams.builder()
                .pickupDatetime("*")
                .dropoffDatetime("*")
                .puLocationID("*")
                .doLocationID("*")
                .groupByPayment(true)
                .vendorID(testVendor)
                .taxiType(testTaxiType)
                .build();

        TripAggregatorService service = new TripAggregatorService(connection);
        List<TripAggregationResult> results = service.aggregate(params);

        assertFalse(results.isEmpty(), "Expected at least one row when filtering by vendor and taxi type with grouping");
        for (TripAggregationResult row : results) {
            assertEquals(testTaxiType, row.taxiType(), "Taxi type should match the filter");
            assertEquals(TripAggregatorService.VENDOR_MAP.get(testVendor), row.vendor(),
                    "Vendor name should match the filter");
        }
    }
}
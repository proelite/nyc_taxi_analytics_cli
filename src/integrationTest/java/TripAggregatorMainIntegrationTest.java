import com.xiaodi.taxi.query.TripAggregator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for TripAggregator.main(...) covering various arg combinations.
 */
public class TripAggregatorMainIntegrationTest {
    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream outContent;

    @BeforeAll
    static void setUp() {
        File dbFile = new File("duck-db/nyc_taxi_combined.duckdb");
        assertTrue(dbFile.exists(),
                "Database file 'duck-db/nyc_taxi_combined.duckdb' not found " +
                        "Please run the Gradle task 'downloadInsertParquetsIntoDBs'.");
    }

    @BeforeEach
    void setUpStreams() {
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    void testMain_withWildcards_noGroupBy() throws Exception {
        String[] args = {"*", "*", "*", "*", "false", "*", "*"};
        TripAggregator.main(args);
        String output = outContent.toString();
        assertTrue(output.contains("Results"), "Should print Results header");
        assertTrue(output.contains("Taxi Type:"), "Should mention Taxi Type");
        assertTrue(output.contains("Vendor:"), "Should mention Vendor");
        assertTrue(output.contains("Payment Type: all"), "Without grouping, Payment Type should be 'all'");
    }

    @Test
    void testMain_withWildcards_groupBy() throws Exception {
        String[] args = {"*", "*", "*", "*", "true", "*", "*"};
        TripAggregator.main(args);
        String output = outContent.toString();
        assertTrue(output.contains("Payment Type:"), "With grouping, should include Payment Type header");
        // Expect at least one specific payment type label
        assertFalse(output.contains("Payment Type: all"), "Grouped output should not default to 'all'");
    }

    @Test
    void testMain_withSpecificFilters_noGroupBy() throws Exception {
        // Use valid date/times, specific IDs, no grouping
        String[] args = {
                "2025-01-01 00:00:00",
                "2025-01-02 00:00:00",
                "1",
                "2",
                "false",
                "3",
                "yellow"
        };
        TripAggregator.main(args);
        String output = outContent.toString();
        assertTrue(output.contains("Taxi Type: yellow"), "Should show Taxi Type 'yellow'");
        assertTrue(output.contains("Vendor:"), "Should show a vendor name");
        assertTrue(output.contains("Payment Type: all"), "Without grouping, Payment Type should be 'all'");
    }

    @Test
    void testMain_withSpecificFilters_groupBy() throws Exception {
        // Use valid date/times, specific IDs, with grouping
        String[] args = {
                "2025-01-01 00:00:00",
                "2025-01-02 00:00:00",
                "1",
                "2",
                "true",
                "3",
                "green"
        };
        TripAggregator.main(args);
        String output = outContent.toString();
        assertTrue(output.contains("Taxi Type: green"), "Should show Taxi Type 'green'");
        assertTrue(output.contains("Vendor:"), "Should show a vendor name");
        assertTrue(output.contains("Payment Type:"), "With grouping, should include Payment Type header");
        // Ensure Payment Type is not always 'all'
        assertFalse(output.contains("Payment Type: all"));
    }

    @Test
    void testMain_invalidArgsLength() throws Exception {
        // Too few arguments
        String[] args = {"*", "*"};
        TripAggregator.main(args);
        String output = outContent.toString();
        // Since main writes to System.err, System.out stays empty
        assertTrue(output.isEmpty(), "No standard output when args are invalid");
    }
}

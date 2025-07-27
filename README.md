# NYC Taxi ETL & Query

This project automates downloading NYC taxi trip Parquet data, loading it into a DuckDB database, and running aggregate queries via an interactive command‑line interface.

## Prerequisites

* **Java 11+** (JDK must be installed and on your PATH)
* **Gradle wrapper** (`gradlew`) or a local Gradle installation
* **Internet access** to install Gradle dependencies and fetch Parquet files 

## Setup & ETL Workflow

1. **Make script executable** (if not already):

   ```bash
   chmod +x gradlew setup.sh
   ```

2. **Run the setup script** to download Parquets and import into DuckDB:

   ```bash
   ./setup.sh
   ```

    * Downloads the specified Parquet files into `parquets/`.
    * Loads them into `duck-db/nyc_taxi_combined.duckdb`
    * Running the script again will replace any existing parquet files. 

## Interactive Query Mode 

1. **Make the script executable** (if not already):

   ```bash
    chmod +x run.sh
   ```

2. **Run the interactive script** to make an aggregate tax trip query

    ```bash
   ./run.sh
    ```

You’ll be prompted for:
    
* **Pickup datetime**: YYYY-MM-DD HH:MM:SS (default: skip)
    
* **Dropoff datetime**: YYYY-MM-DD HH:MM:SS (default: skip)
    
* **Pickup location ID** An integer (e.g. 1)
    
* **Dropoff location ID** An integer (e.g. 2)
    
* **Group by payment type?** Enter true or false (default: true)
    
* **Vendor ID** Enter an integer (e.g. 3), or leave blank to include all vendors
    
* **Taxi type** Enter yellow, green, or both (default: both)
    
The script validates each entry and will re-prompt until you provide a correctly formatted value.

Example output:

**Taxi Type:** yellow  
**Vendor:** Creative Mobile Technologies, LLC  
**Payment Type:** No charge  
**Min Fare:** 0.0  
**Max Fare:** 98.0  
**Count of Trips:** 1114  
**Total Toll Fare Sum:** 40.949999999999996  
**Total Fare Sum:** 15591.630000000001

---

**Taxi Type:** yellow  
**Vendor:** Creative Mobile Technologies, LLC  
**Payment Type:** Dispute  
**Min Fare:** 0.0  
**Max Fare:** 70.0  
**Count of Trips:** 754  
**Total Toll Fare Sum:** 9.0  
**Total Fare Sum:** 10234.3

## Project Structure

```
├── build.gradle.kts           # Gradle build configuration
├── setup.sh                   # Shell script to run ETL steps
├── run.sh                     # Interactive shell script to execute aggregation query
├── parquets/                  # Directory where Parquet files will be downloaded
├── duck-db/                   # Directory containing the DuckDB database file
│   └── nyc_taxi_combined.duckdb
└── src/main/java
    └── com/xiaodi/taxi
        ├── etl
        │   ├── ParquetsDownloader.java    # Downloads Parquet files from hardcoded links within the file. 
        │   └── ParquetsDBInserter.java     # Inserts Parquets into DuckDB
        └── query
            └── TripAggregator.java        # Executes aggregate query on DuckDB
```

## Gradle Tasks

| Task                            | Description                                                |
| ------------------------------- | ---------------------------------------------------------- |
| `downloadParquets`              | Downloads the Parquet files into `parquets/`.              |
| `insertParquetsIntoDBs`         | Scans `parquets/` and loads data into the DuckDB file.     |
| `downloadInsertParquetsIntoDBs` | Runs both download and insert steps in sequence.           |
| `executeQuery`                  | Executes the Java query (`TripAggregator`) against DuckDB. |

## Running Queries Manually

By default, `executeQuery` is configured with sample parameters. To run it:

```bash
./gradlew executeQuery
```

### Default Parameters

These defaults are defined in `build.gradle.kts` under the `executeQuery` task’s `args(...)`:

1. **pickupDatetime**:  `2025-06-01 00:00:00`
2. **dropoffDatetime**: `2025-06-30 23:59:59`
3. **puLocationID**:     `1`
4. **doLocationID**:     `2`
5. **groupByPaymentType**: `true`
6. **vendorID**:         `1`
7. **taxiType**:         `yellow`

### Customizing Query Parameters

Replace placeholders with your desired values. Use `*` to disable filtering by vendorID or taxiType.

## License
The MIT License (MIT)

Copyright (c) 2025 Xiaodi Huang

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.


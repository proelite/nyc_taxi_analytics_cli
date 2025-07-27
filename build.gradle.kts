plugins {
    id("java")
}

group = "org.xiaodi"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.duckdb:duckdb_jdbc:0.9.2")
    compileOnly("org.projectlombok:lombok:1.18.30") // Use the latest version
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    testImplementation(platform("org.junit:junit-bom:5.10.2")) // Use the latest stable version
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.+")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.30")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    // Optional: Configure test logging or other settings
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

tasks.register<JavaExec>("downloadParquets") {
    group = "etl"
    description = "Fetches parquets from links"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.xiaodi.taxi.utils.ParquetsDownloader")
}

tasks.register<JavaExec>("insertParquetsIntoDBs") {
    group = "etl"
    description = "Scans parquets and insert into the DuckDB database"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.xiaodi.taxi.etl.ParquetsDBInserter")
    jvmArgs(
        "--enable-native-access=ALL-UNNAMED", // e.g. for DuckDB native load
    )
}

tasks.register("downloadInsertParquetsIntoDBs") {
    group = "etl"
    description = "Download parquets and insert into the DuckDb database"
    dependsOn("downloadParquets", "insertParquetsIntoDBs")
}

tasks.register<JavaExec>("executeQuery") {
    group = "etl"
    description = "Execute query against the trip database"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.xiaodi.taxi.query.TripAggregator")
    // default positional args to your Java main()
    args(
        "*",   // pickupDatetime
        "2025-05-01 00:00:00",   // dropoffDatetime
        "*",                  // puLocationID
        "*",                  // doLocationID
        "true",                  // groupByPaymentType
        "*",                     // vendorID
        "*",                // taxiType
    )
    jvmArgs(
        "--enable-native-access=ALL-UNNAMED", // e.g. for DuckDB native load
    )
}
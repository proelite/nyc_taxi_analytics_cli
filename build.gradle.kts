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
    testImplementation(platform("org.jun.setit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("downloadParquets") {
    group = "etl"
    description = "Fetches parquets from links"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.xiaodi.taxi.etl.ParquetsDownloader")
}

tasks.register<JavaExec>("insertParquetsIntoDBs") {
    group = "etl"
    description = "Scans parquets and insert into the DuckDB database"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.xiaodi.taxi.etl.ParquetsDBInserter")
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
        "false",                  // groupByPaymentType
        "*",                     // vendorID
        "*",                // taxiType
    )
    jvmArgs(
        "--enable-native-access=ALL-UNNAMED", // e.g. for DuckDB native load
    )
}
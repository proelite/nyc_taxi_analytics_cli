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
    implementation("org.jetbrains:annotations:23.0.0")
    implementation("org.apiguardian:apiguardian-api:1.1.2")
    testImplementation(platform("org.junit:junit-bom:5.10.2")) // Use the latest stable version
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.+")
    testImplementation("org.mockito:mockito-junit-jupiter:5.12.0")
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
    mainClass.set("com.xiaodi.taxi.etl.DBInserter")
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
        "*",   // dropoffDatetime
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
tasks.named<Test>("test") {
    useJUnitPlatform()
    // Optional: Configure test logging or other settings
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

sourceSets {
    val integrationTest by creating {
        java.srcDir("src/integrationTest/java")
        resources.srcDir("src/integrationTest/resources")
        compileClasspath += sourceSets["main"].output + configurations["testRuntimeClasspath"]
        runtimeClasspath += output + compileClasspath
    }
}

val integrationTest = tasks.register<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    include("**/*IntegrationTest.class", "**/*IT.class", "**/*Test.class")
    shouldRunAfter(tasks.test)
    useJUnitPlatform()
}

// hook it into the build lifecycle
tasks.check {
    dependsOn(integrationTest)
}
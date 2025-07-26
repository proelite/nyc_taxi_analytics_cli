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
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("runEtl") {
    group = "etl"
    description = "Runs the ETL job to generate the DuckDB databases"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.xiaodi.taxi.etl.ParquetsIntoDuckDBs")
}

tasks.register<JavaExec>("mergeDuckDbs") {
    group = "etl"
    description = "Combines individual DuckDBs into a unified table"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.xiaodi.taxi.etl.MergeDuckDBs")
}

tasks.register<JavaExec>("runFullEtl") {
    group = "etl"
    description = "Runs the ETL split and combine jobs in sequence"

    dependsOn("splitParquets", "combineDuckdbs")

    // Optional: enforce ordering
    doLast {
        println( "✅ All ETL steps completed.")
    }
}
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
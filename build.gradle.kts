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
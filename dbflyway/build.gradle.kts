dependencies {
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.flywaydb:flyway-core:10.6.0")
    implementation("org.flywaydb:flyway-database-postgresql:10.6.0")
    runtimeOnly("org.postgresql:postgresql:42.7.1")

    implementation("org.testcontainers:postgresql:1.19.3")
}

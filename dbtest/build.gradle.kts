dependencies {
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.flywaydb:flyway-core:10.6.0")
    implementation("org.flywaydb:flyway-database-postgresql:10.6.0")
    runtimeOnly("org.postgresql:postgresql:42.7.1")

    implementation("org.testcontainers:postgresql:1.19.4")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testImplementation("org.assertj:assertj-core:3.25.2")

    implementation(project(":verdityper"))
}

dependencies {
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.flywaydb:flyway-core:10.12.0")
    implementation("org.flywaydb:flyway-database-postgresql:10.12.0")
    runtimeOnly("org.postgresql:postgresql:42.7.3")

    implementation("org.testcontainers:postgresql:1.19.7")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testImplementation("org.assertj:assertj-core:3.25.3")

    implementation(project(":verdityper"))
}

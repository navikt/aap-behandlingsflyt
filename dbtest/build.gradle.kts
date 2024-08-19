dependencies {
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.flywaydb:flyway-database-postgresql:10.17.1")
    runtimeOnly("org.postgresql:postgresql:42.7.3")

    implementation("org.testcontainers:postgresql:1.20.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.0")
    testImplementation("org.assertj:assertj-core:3.26.3")

    implementation(project(":verdityper"))
}

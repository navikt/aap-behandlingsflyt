dependencies {
    implementation("dev.forst:ktor-openapi-generator:0.6.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")

    implementation(project(":verdityper"))
    implementation(project(":dbconnect"))
    implementation(project(":dbflyway"))
    implementation(project(":httpklient"))

    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.flywaydb:flyway-core:10.7.2")
    implementation("org.flywaydb:flyway-database-postgresql:10.8.1")
    runtimeOnly("org.postgresql:postgresql:42.7.2")

    testImplementation(project(":dbtestdata"))
    testImplementation(project(":dbtest"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testImplementation("org.assertj:assertj-core:3.25.3")
    testImplementation("org.testcontainers:postgresql:1.19.4")
    testImplementation(kotlin("test"))
}

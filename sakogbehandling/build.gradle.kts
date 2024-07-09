val ktorVersion = "2.3.12"

dependencies {
    implementation("dev.forst:ktor-openapi-generator:0.6.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")

    implementation(project(":verdityper"))
    implementation(project(":dbconnect"))
    implementation(project(":dbflyway"))
    implementation(project(":httpklient"))

    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.flywaydb:flyway-database-postgresql:10.15.2")
    runtimeOnly("org.postgresql:postgresql:42.7.3")

    testImplementation(project(":dbtestdata"))
    testImplementation(project(":dbtest"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.3")
    testImplementation("org.assertj:assertj-core:3.26.0")
    testImplementation("org.testcontainers:postgresql:1.19.8")
    testImplementation(kotlin("test"))
    testImplementation(project(":lib-test"))
}

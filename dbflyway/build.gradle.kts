dependencies {
    implementation(project(":infrastructure"))
    implementation("org.flywaydb:flyway-database-postgresql:10.17.1")
    runtimeOnly("org.postgresql:postgresql:42.7.3")
}

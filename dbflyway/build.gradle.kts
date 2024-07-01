dependencies {
    implementation(project(":infrastructure"))
    implementation("org.flywaydb:flyway-database-postgresql:10.15.2")
    runtimeOnly("org.postgresql:postgresql:42.7.3")
}

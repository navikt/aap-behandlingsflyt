val jacksonVersjon = "2.19.2"

plugins {
    id("behandlingsflyt.conventions")
}

dependencies {
    implementation(project(":behandlingsflyt"))

    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation(libs.verdityper)
    implementation(libs.dbconnect)
    implementation(libs.dbmigrering)
    implementation(libs.httpklient)
    implementation(libs.infrastructure)
    implementation(libs.tidslinje)
    implementation("org.flywaydb:flyway-database-postgresql:11.10.4")
    implementation("io.getunleash:unleash-client-java:11.0.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersjon")
    runtimeOnly("org.postgresql:postgresql:42.7.7")

    testImplementation(project(":lib-test"))
    testImplementation(libs.dbtest)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.bundles.junit)
}

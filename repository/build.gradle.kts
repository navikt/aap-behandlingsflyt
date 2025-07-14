plugins {
    id("behandlingsflyt.conventions")
}

val junitVersjon = "5.13.3"

dependencies {
    implementation(project(":behandlingsflyt"))

    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation(libs.verdityper)
    implementation(libs.dbconnect)
    implementation(libs.dbmigrering)
    implementation(libs.httpklient)
    implementation(libs.infrastructure)
    implementation(libs.tidslinje)
    implementation("org.flywaydb:flyway-database-postgresql:11.10.2")
    implementation("io.getunleash:unleash-client-java:11.0.2")
    runtimeOnly("org.postgresql:postgresql:42.7.7")

    testImplementation(project(":lib-test"))
    testImplementation(libs.dbtest)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersjon")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersjon")
    testImplementation("org.assertj:assertj-core:3.27.3")
}

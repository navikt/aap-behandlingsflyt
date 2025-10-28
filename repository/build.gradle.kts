
plugins {
    id("behandlingsflyt.conventions")
}

dependencies {
    implementation(project(":behandlingsflyt"))

    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.21.0")
    implementation("ch.qos.logback:logback-classic:1.5.20")
    implementation(libs.verdityper)
    implementation(libs.dbconnect)
    implementation(libs.dbmigrering)
    implementation(libs.httpklient)
    implementation(libs.infrastructure)
    implementation(libs.tidslinje)
    implementation("org.flywaydb:flyway-database-postgresql:11.15.0")
    implementation("io.getunleash:unleash-client-java:11.1.1")
    runtimeOnly("org.postgresql:postgresql") // låst versjon i root build.gradle.kts
    implementation("no.bekk.bekkopen:nocommons:0.16.0")

    testImplementation(project(":lib-test"))
    testImplementation(libs.dbtest)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.bundles.junit)
}


plugins {
    id("behandlingsflyt.conventions")
}

dependencies {
    implementation(project(":behandlingsflyt"))

    implementation(libs.opentelemetryInstrumentationAnnotations)
    implementation(libs.logbackClassic)
    implementation(libs.verdityper)
    implementation(libs.dbconnect)
    implementation(libs.dbmigrering)
    implementation(libs.httpklient)
    implementation(libs.infrastructure)
    implementation(libs.tidslinje)
    implementation(libs.apiInternKontrakt)

    implementation(libs.caffeine)
    implementation(libs.flywayDatabasePostgresql)
    implementation("io.getunleash:unleash-client-java:11.1.1")
    runtimeOnly(libs.postgresql) // låst versjon i root build.gradle.kts
    implementation("no.bekk.bekkopen:nocommons:0.16.0")

    testImplementation(project(":lib-test"))
    testImplementation(libs.dbtest)
    testImplementation(libs.mockk)

    testRuntimeOnly(libs.junitPlatformLauncher)
    testImplementation(libs.bundles.junit)
    testImplementation(kotlin("test"))
}

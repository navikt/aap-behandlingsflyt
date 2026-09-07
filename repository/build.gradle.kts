plugins {
    id("aap.conventions")
}

dependencies {
    implementation(project(":behandlingsflyt"))

    implementation(kelvinLibs.coroutines.core)
    implementation(kelvinLibs.opentelemetry.annotations)
    implementation(kelvinLibs.logback.classic)
    implementation(libs.verdityper)
    implementation(libs.dbconnect)
    implementation(libs.dbmigrering)
    implementation(libs.httpklient)
    implementation(libs.infrastructure)
    implementation(libs.tidslinje)
    implementation(libs.apiInternKontrakt)

    implementation(kelvinLibs.caffeine)
    implementation(kelvinLibs.unleash.client.java)
    implementation(libs.bekk.open.nocommons)

    testImplementation(project(":lib-test"))
    testImplementation(libs.dbtest)
    testImplementation(kelvinLibs.mockk)

    testImplementation(kelvinLibs.bundles.junit)
    testImplementation(kotlin("test"))
}

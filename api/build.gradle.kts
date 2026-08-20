
plugins {
    id("aap.conventions")
    id("io.github.androa.gradle.plugin.avro") version "0.0.12"
}

dependencies {
    api(project(":behandlingsflyt"))
    implementation(libs.dbconnect)
    implementation(libs.infrastructure)
    implementation(libs.server)
    implementation(libs.motorApi)
    implementation(libs.verdityper)
    implementation(libs.tidslinje)
    implementation(libs.avro)
    implementation(libs.kafkaAvroSerializer)
    implementation(libs.kafkaClients)
    api(libs.tilgangPlugin)
    api(libs.tilgangKontrakt)
    compileOnly(libs.ktorHttpJvm)

    testImplementation(libs.httpklient)
    testImplementation(libs.dbtest)
    testImplementation(libs.bundles.junit)
    testImplementation(libs.ktorServerTestHost)
    constraints {
        implementation("commons-codec:commons-codec:1.22.1")
    }
    testImplementation(libs.ktorClientContentNegotiation)
    testImplementation(libs.mockOauth2Server)
    testImplementation(project(":lib-test"))
    testImplementation(project(":repository"))
    testImplementation(libs.mockk)
}

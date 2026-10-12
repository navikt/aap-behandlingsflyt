
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
    implementation(kelvinLibs.avro)
    implementation(kelvinLibs.kafka.avro.serializer)
    implementation(kelvinLibs.kafka.clients)
    api(libs.tilgangPlugin)
    api(libs.tilgangKontrakt)
    compileOnly(kelvinLibs.ktor.http.jvm)

    testImplementation(libs.httpklient)
    testImplementation(libs.dbtest)
    testImplementation(kelvinLibs.bundles.junit)
    testImplementation(kelvinLibs.ktor.server.test.host)
    testImplementation(kelvinLibs.ktor.client.content.negotiation)
    testImplementation(kelvinLibs.mock.oauth2.server)
    testImplementation(project(":lib-test"))
    testImplementation(project(":repository"))
    testImplementation(kelvinLibs.mockk)
}

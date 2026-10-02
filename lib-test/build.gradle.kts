plugins {
    id("aap.conventions")
}

dependencies {
    implementation(project(":behandlingsflyt"))
    implementation(project(":repository"))
    implementation(project(":kontrakt"))
    implementation(libs.brevKontrakt)
    implementation(libs.tilgangKontrakt)
    implementation(libs.httpklient)
    implementation(libs.verdityper)
    implementation(libs.tidslinje)
    implementation(libs.dbconnect)

    implementation(kelvinLibs.ktor.server.content.negotiation)

    implementation(kelvinLibs.ktor.server.netty)
    implementation(kelvinLibs.ktor.server.status.pages)

    implementation(kelvinLibs.ktor.serialization.jackson)
    implementation(kelvinLibs.jackson.databind)
    implementation(kelvinLibs.jackson.datatype.jsr310)

    implementation(kelvinLibs.logback.classic)

    implementation(kelvinLibs.nimbus.jose.jwt)

    implementation(kelvinLibs.bundles.junit)
}

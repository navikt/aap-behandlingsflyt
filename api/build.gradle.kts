
plugins {
    id("behandlingsflyt.conventions")
}

val mockOAuth2ServerVersion = "2.3.0"
val kafkaVersion = "4.1.0"

dependencies {
    api(project(":behandlingsflyt"))
    implementation(libs.dbconnect)
    implementation(libs.infrastructure)
    implementation(libs.server)
    implementation(libs.motorApi)
    implementation(libs.verdityper)
    implementation(libs.tidslinje)
    implementation("org.apache.kafka:kafka-clients:${kafkaVersion}")
    api(libs.tilgangPlugin)
    api(libs.tilgangKontrakt)
    compileOnly(libs.ktorHttpJvm)

    testImplementation(libs.httpklient)
    testImplementation(libs.bundles.junit)
    testImplementation(libs.ktorServerTestHost)
    constraints {
        implementation("commons-codec:commons-codec:1.19.0")
    }
    testImplementation(libs.ktorClientContentNegotiation)
    testImplementation("no.nav.security:mock-oauth2-server:$mockOAuth2ServerVersion")
    testImplementation(project(":lib-test"))
    testImplementation(project(":repository"))
}

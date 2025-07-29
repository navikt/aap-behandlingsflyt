val jacksonVersjon = "2.19.2"

plugins {
    id("behandlingsflyt.conventions")
}

val mockOAuth2ServerVersion = "2.2.1"
val kafkaVersion = "4.0.0"

dependencies {
    api(project(":behandlingsflyt"))
    implementation(libs.dbconnect)
    implementation(libs.infrastructure)
    implementation(libs.httpklient)
    implementation(libs.server)
    implementation(libs.motorApi)
    implementation(libs.verdityper)
    implementation(libs.tidslinje)
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersjon")
    implementation("org.apache.kafka:kafka-clients:${kafkaVersion}")
    api(libs.tilgangPlugin)
    api(libs.tilgangKontrakt)
    compileOnly(libs.ktorHttpJvm)

    testImplementation(libs.bundles.junit)
    testImplementation(libs.ktorServerTestHost)
    constraints {
        implementation("commons-codec:commons-codec:1.19.0")
    }
    testImplementation(libs.ktorClientContentNegotiation)
    testImplementation("no.nav.security:mock-oauth2-server:$mockOAuth2ServerVersion")
    testImplementation(project(":lib-test"))
}

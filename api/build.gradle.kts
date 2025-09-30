
plugins {
    id("behandlingsflyt.conventions")
    id("com.github.davidmc24.gradle.plugin.avro") version "1.5.0"
}

val mockOAuth2ServerVersion = "3.0.0"
val kafkaVersion = "4.1.0"

dependencies {
    api(project(":behandlingsflyt"))
    implementation(libs.dbconnect)
    implementation(libs.infrastructure)
    implementation(libs.server)
    implementation(libs.motorApi)
    implementation(libs.verdityper)
    implementation(libs.tidslinje)
    implementation("org.apache.avro:avro:1.11.2")
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

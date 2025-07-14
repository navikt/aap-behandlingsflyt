plugins {
    id("behandlingsflyt.conventions")
}

val ktorVersion = "3.2.1"
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
    implementation("org.apache.kafka:kafka-clients:${kafkaVersion}")
    api(libs.tilgangPlugin)
    api(libs.tilgangKontrakt)
    compileOnly("io.ktor:ktor-http-jvm:$ktorVersion")

    testImplementation(libs.bundles.junit)
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    constraints {
        implementation("commons-codec:commons-codec:1.18.0")
    }
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    testImplementation("no.nav.security:mock-oauth2-server:$mockOAuth2ServerVersion")
    testImplementation(project(":lib-test"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.27.3")
}

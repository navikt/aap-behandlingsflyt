plugins {
    id("behandlingsflyt.conventions")
}

val komponenterVersjon = "1.0.277"
val ktorVersion = "3.2.0"
val tilgangVersjon = "1.0.87"
val junitVersjon = "5.13.2"
val mockOAuth2ServerVersion = "2.2.1"
val kafkaVersion = "4.0.0"

dependencies {
    api(project(":behandlingsflyt"))
    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:server:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:motor-api:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:verdityper:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:tidslinje:$komponenterVersjon")
    implementation("org.apache.kafka:kafka-clients:${kafkaVersion}")
    api("no.nav.aap.tilgang:plugin:$tilgangVersjon")
    api("no.nav.aap.tilgang:api-kontrakt:$tilgangVersjon")
    compileOnly("io.ktor:ktor-http-jvm:$ktorVersion")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersjon")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersjon")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersjon")
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

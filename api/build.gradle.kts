plugins {
    id("behandlingsflyt.conventions")
}

val komponenterVersjon = "1.0.242"
val ktorVersion = "3.1.3"
val tilgangVersjon = "1.0.64"
val junitVersjon = "5.12.2"
val mockOAuth2ServerVersion = "2.1.11"

dependencies {
    api(project(":behandlingsflyt"))
    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:server:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:motor-api:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:verdityper:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:tidslinje:$komponenterVersjon")
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

val ktorVersion = "3.0.1"
val komponenterVersjon = "1.0.63"
val tilgangVersjon = "0.0.35"

plugins {
    id("behandlingsflyt.conventions")
}

dependencies {
    implementation(project(":integrasjon"))
    implementation(project(":verdityper"))
    implementation(project(":sakogbehandling"))
    implementation(project(":faktagrunnlag"))
    implementation("no.nav.aap.tilgang:plugin:$tilgangVersjon")
    implementation("no.nav.aap.tilgang:api-kontrakt:$tilgangVersjon")
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    implementation("no.nav:ktor-openapi-generator:1.0.46")
    implementation("io.ktor:ktor-http-jvm:$ktorVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.3")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation(project(":lib-test"))
}
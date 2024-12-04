plugins {
    id("behandlingsflyt.conventions")
}

val komponenterVersjon = "1.0.72"
val ktorVersion = "3.0.2"
val tilgangVersjon = "0.0.46"

dependencies {
    implementation(project(":behandlingsflyt"))
    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:server:$komponenterVersjon")
    api("no.nav.aap.tilgang:plugin:$tilgangVersjon")
    api("no.nav.aap.tilgang:api-kontrakt:$tilgangVersjon")
    compileOnly("io.ktor:ktor-http-jvm:$ktorVersion")
}
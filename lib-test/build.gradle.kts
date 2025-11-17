plugins {
    id("behandlingsflyt.conventions")
}

val jacksonVersjon = "2.20.1"

dependencies {
    implementation(project(":behandlingsflyt"))
    implementation(project(":repository"))
    implementation(project(":kontrakt"))
    implementation("no.nav.aap.brev:kontrakt:0.0.184")
    implementation(libs.tilgangKontrakt)
    implementation(libs.httpklient)
    implementation(libs.verdityper)
    implementation(libs.tidslinje)
    implementation(libs.dbconnect)

    implementation(libs.ktorServerContentNegotation)

    implementation(libs.ktorServerNetty)
    implementation(libs.ktorServerStatusPages)

    implementation(libs.ktorSerializationJackson)
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersjon")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersjon")

    implementation("ch.qos.logback:logback-classic:1.5.21")

    implementation("com.nimbusds:nimbus-jose-jwt:10.6")

    implementation(libs.bundles.junit)
}

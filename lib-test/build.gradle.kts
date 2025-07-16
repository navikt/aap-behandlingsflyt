plugins {
    id("behandlingsflyt.conventions")
}

val jacksonVersjon = "2.19.1"

dependencies {
    implementation(project(":behandlingsflyt"))
    implementation(project(":repository"))
    implementation(project(":kontrakt"))
    implementation("no.nav.aap.brev:kontrakt:0.0.130")
    implementation(libs.tilgangKontrakt)
    implementation(libs.httpklient)
    implementation(libs.verdityper)
    implementation(libs.tidslinje)
    implementation(libs.dbconnect)

    implementation(libs.ktorServerContentNegotation)

    implementation(libs.ktorServerNetty)
    constraints {
        implementation("io.netty:netty-common:4.2.3.Final")
    }
    implementation(libs.ktorServerStatusPages)

    implementation(libs.ktorSerializationJackson)
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersjon")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersjon")

    implementation("ch.qos.logback:logback-classic:1.5.18")

    implementation("com.nimbusds:nimbus-jose-jwt:10.3.1")

    implementation(libs.bundles.junit)
}

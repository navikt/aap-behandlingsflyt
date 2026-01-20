plugins {
    id("aap.conventions")
}

dependencies {
    implementation(project(":behandlingsflyt"))
    implementation(project(":repository"))
    implementation(project(":kontrakt"))
    implementation(libs.brevKontrakt)
    implementation(libs.tilgangKontrakt)
    implementation(libs.httpklient)
    implementation(libs.verdityper)
    implementation(libs.tidslinje)
    implementation(libs.dbconnect)

    implementation(libs.ktorServerContentNegotation)

    implementation(libs.ktorServerNetty)
    implementation(libs.ktorServerStatusPages)

    implementation(libs.ktorSerializationJackson)
    implementation(libs.jacksonDatabind)
    implementation(libs.jacksonDatatypeJsr310)

    implementation(libs.logbackClassic)

    implementation(libs.joseJwt)

    implementation(libs.bundles.junit)
}

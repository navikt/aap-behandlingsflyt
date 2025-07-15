plugins {
    id("behandlingsflyt.conventions")
}

val ktorVersion = "3.2.1"

dependencies {
    api(project(":kontrakt"))
    implementation("io.micrometer:micrometer-registry-prometheus:1.15.2")
    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("net.logstash.logback:logstash-logback-encoder:8.1")

    api(libs.tilgangPlugin)
    api(libs.tilgangKontrakt)
    api("no.nav.aap.brev:kontrakt:0.0.130")
    api("no.nav.aap.meldekort:kontrakt:0.0.75")
    api(libs.motor)
    api(libs.gateway)
    api(libs.utbetalKontrakt)
    implementation(libs.dbconnect)
    // TODO: fjern denne avhengigheten når alle RestClient-instanser er i repository-modulen
    implementation(libs.httpklient)
    implementation(libs.json)
    implementation(libs.infrastructure)
    implementation(libs.verdityper)
    implementation(libs.tidslinje)
    implementation(kotlin("reflect"))
    // TODO: fjern når alle api er flyttet til api-modul
    compileOnly("io.ktor:ktor-http-jvm:$ktorVersion")
    implementation("org.flywaydb:flyway-database-postgresql:11.10.2")
    runtimeOnly("org.postgresql:postgresql:42.7.7")


    testImplementation(project(":lib-test"))
    testImplementation(project(":repository"))
    implementation(libs.dbtest)
    implementation(libs.motorTestUtils)
    testImplementation(libs.bundles.junit)

    testImplementation("org.testcontainers:postgresql:1.21.3")
    constraints {
        implementation("org.apache.commons:commons-compress:1.27.1") {
            because("https://github.com/advisories/GHSA-4g9r-vxhx-9pgx")
        }
    }
    testImplementation("io.mockk:mockk:1.14.4")
    testImplementation(kotlin("test"))
}

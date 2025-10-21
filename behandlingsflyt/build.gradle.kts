
plugins {
    id("behandlingsflyt.conventions")
}

dependencies {
    api(project(":kontrakt"))
    implementation("io.micrometer:micrometer-registry-prometheus:1.15.5")
    implementation("ch.qos.logback:logback-classic:1.5.20")
    implementation("net.logstash.logback:logstash-logback-encoder:8.1")
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.21.0")

    api(libs.tilgangPlugin)
    api(libs.tilgangKontrakt)
    api("no.nav.aap.brev:kontrakt:0.0.163")
    api("no.nav.aap.meldekort:kontrakt:0.0.139")
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
    implementation("org.flywaydb:flyway-database-postgresql:11.14.1")
    runtimeOnly("org.postgresql:postgresql") // låst versjon i root build.gradle.kts


    testImplementation(project(":lib-test"))
    testImplementation(project(":repository"))
    implementation(libs.dbtest)
    implementation(libs.motorTestUtils)
    testImplementation(libs.bundles.junit)

    testImplementation("org.testcontainers:postgresql:1.21.3")
    constraints {
        implementation("org.apache.commons:commons-compress:1.28.0") {
            because("https://github.com/advisories/GHSA-4g9r-vxhx-9pgx")
        }
    }
    testImplementation("io.mockk:mockk:1.14.6")
    testImplementation(kotlin("test"))
}

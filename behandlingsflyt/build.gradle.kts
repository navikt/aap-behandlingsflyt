plugins {
    id("behandlingsflyt.conventions")
}

val komponenterVersjon = "1.0.237"
val ktorVersion = "3.1.3"
val tilgangVersjon = "1.0.58"
val utbetalVersjon = "0.0.40"
val junitVersjon = "5.12.2"

dependencies {
    api(project(":kontrakt"))
    implementation("io.micrometer:micrometer-registry-prometheus:1.14.6")
    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("net.logstash.logback:logstash-logback-encoder:8.1")

    api("no.nav.aap.tilgang:plugin:$tilgangVersjon")
    api("no.nav.aap.tilgang:api-kontrakt:$tilgangVersjon")
    api("no.nav.aap.brev:kontrakt:0.0.106")
    api("no.nav.aap.meldekort:kontrakt:0.0.34")
    api("no.nav.aap.kelvin:motor:$komponenterVersjon")
    api("no.nav.aap.kelvin:gateway:$komponenterVersjon")
    api("no.nav.aap.utbetal:api-kontrakt:$utbetalVersjon")
    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    // TODO: fjern denne avhengigheten når alle RestClient-instanser er i repository-modulen
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:json:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:verdityper:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:tidslinje:$komponenterVersjon")
    implementation(kotlin("reflect"))
    // TODO: fjern når alle api er flyttet til api-modul
    compileOnly("io.ktor:ktor-http-jvm:$ktorVersion")
    implementation("org.flywaydb:flyway-database-postgresql:11.8.1")
    runtimeOnly("org.postgresql:postgresql:42.7.5")


    testImplementation(project(":lib-test"))
    testImplementation(project(":repository"))
    testImplementation("no.nav.aap.kelvin:dbtest:$komponenterVersjon")
    testImplementation("no.nav.aap.kelvin:motor-test-utils:$komponenterVersjon")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersjon")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersjon")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersjon")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("org.testcontainers:postgresql:1.21.0")
    constraints {
        implementation("org.apache.commons:commons-compress:1.27.1") {
            because("https://github.com/advisories/GHSA-4g9r-vxhx-9pgx")
        }
    }
    testImplementation("io.mockk:mockk:1.14.2")
    testImplementation(kotlin("test"))
}

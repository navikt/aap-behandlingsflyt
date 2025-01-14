plugins {
    id("behandlingsflyt.conventions")
}

val komponenterVersjon = "1.0.101"
val ktorVersion = "3.0.3"
val tilgangVersjon = "0.0.86"
val junitVersjon = "5.11.4"

dependencies {
    api(project(":kontrakt"))
    implementation("io.micrometer:micrometer-registry-prometheus:1.14.3")
    implementation("ch.qos.logback:logback-classic:1.5.16")
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")

    api("no.nav.aap.tilgang:plugin:$tilgangVersjon")
    api("no.nav.aap.tilgang:api-kontrakt:$tilgangVersjon")
    implementation("no.nav.aap.brev:kontrakt:0.0.51")
    api("no.nav.aap.kelvin:motor:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbmigrering:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:json:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:verdityper:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:server:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:tidslinje:$komponenterVersjon")
    implementation(kotlin("reflect"))
    compileOnly("io.ktor:ktor-http-jvm:$ktorVersion")
    implementation("com.zaxxer:HikariCP:6.2.1")
    implementation("org.flywaydb:flyway-database-postgresql:11.1.1")
    runtimeOnly("org.postgresql:postgresql:42.7.4")


    testImplementation(project(":lib-test"))
    testImplementation(project(":repository"))
    testImplementation("no.nav.aap.kelvin:dbtest:$komponenterVersjon")
    testImplementation("no.nav.aap.kelvin:motor-test-utils:$komponenterVersjon")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersjon")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersjon")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersjon")
    testImplementation("org.assertj:assertj-core:3.27.2")
    testImplementation("org.testcontainers:postgresql:1.20.4")
    constraints {
        implementation("org.apache.commons:commons-compress:1.27.1") {
            because("https://github.com/advisories/GHSA-4g9r-vxhx-9pgx")
        }
    }
    testImplementation("io.mockk:mockk:1.13.16")
    testImplementation(kotlin("test"))
}

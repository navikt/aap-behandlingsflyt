val ktorVersion = "3.0.0"
val komponenterVersjon = "1.0.40"
val tilgangVersjon = "0.0.24"

plugins {
    id("behandlingsflyt.conventions")
}

dependencies {
    implementation("no.nav:ktor-openapi-generator:1.0.46")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.0")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")

    implementation(project(":verdityper"))
    implementation(project(":dbflyway"))
    implementation(project(":integrasjon"))

    api("no.nav.aap.tilgang:plugin:$tilgangVersjon")
    api("no.nav.aap.tilgang:api-kontrakt:$tilgangVersjon")
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbmigrering:$komponenterVersjon")
    implementation("com.zaxxer:HikariCP:6.0.0")
    implementation("org.flywaydb:flyway-database-postgresql:10.18.2")
    runtimeOnly("org.postgresql:postgresql:42.7.4")

    testImplementation(project(":dbtestdata"))
    testImplementation("no.nav.aap.kelvin:dbtest:$komponenterVersjon")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.3")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("org.testcontainers:postgresql:1.20.3")
    testImplementation(kotlin("test"))
    testImplementation(project(":lib-test"))
}

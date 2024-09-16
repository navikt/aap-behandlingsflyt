val ktorVersion = "2.3.12"
val komponenterVersjon = "0.0.53"
val tilgangVersjon = "0.0.2"

dependencies {
    implementation("no.nav:ktor-openapi-generator:1.0.22")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")

    implementation(project(":verdityper"))
    implementation(project(":dbflyway"))
    implementation(project(":httpklient"))
    
    implementation("no.nav.aap.tilgang:plugin:$tilgangVersjon")
    implementation("no.nav.aap.tilgang:api-kontrakt:$tilgangVersjon")
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbmigrering:$komponenterVersjon")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.flywaydb:flyway-database-postgresql:10.17.3")
    runtimeOnly("org.postgresql:postgresql:42.7.4")

    testImplementation(project(":dbtestdata"))
    testImplementation("no.nav.aap.kelvin:dbtest:$komponenterVersjon")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.0")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("org.testcontainers:postgresql:1.20.1")
    testImplementation(kotlin("test"))
    testImplementation(project(":lib-test"))
}

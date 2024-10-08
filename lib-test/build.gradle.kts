val ktorVersion = "2.3.12"
val komponenterVersjon = "1.0.11"

plugins {
    id("behandlingsflyt.conventions")
}

dependencies {
    implementation(project(":sakogbehandling"))
    implementation(project(":faktagrunnlag"))
    implementation(project(":verdityper"))
    implementation(project(":integrasjon"))
    implementation(project(":app"))
    implementation("no.nav.aap.tilgang:api-kontrakt:0.0.13")
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")

    implementation("no.nav.aap.statistikk:api-kontrakt:0.0.15")

    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.0")

    implementation("ch.qos.logback:logback-classic:1.5.8")

    implementation("com.nimbusds:nimbus-jose-jwt:9.41.1")

    implementation("org.junit.jupiter:junit-jupiter-api:5.11.2")

    api(project(":dbtestdata"))
}
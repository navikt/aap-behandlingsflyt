val ktorVersion = "3.0.3"
val komponenterVersjon = "1.0.101"
val tilgangVersjon = "0.0.88"
val junitVersjon = "5.11.4"

plugins {
    id("behandlingsflyt.conventions")
}

dependencies {
    implementation(project(":behandlingsflyt"))
    implementation(project(":repository"))
    implementation(project(":kontrakt"))
    implementation("no.nav.aap.brev:kontrakt:0.0.52")
    implementation("no.nav.aap.tilgang:api-kontrakt:$tilgangVersjon")
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:verdityper:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    constraints {
        implementation("io.netty:netty-common:4.1.117.Final")
    }
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")

    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")

    implementation("ch.qos.logback:logback-classic:1.5.16")

    implementation("com.nimbusds:nimbus-jose-jwt:10.0.1")

    implementation("org.junit.jupiter:junit-jupiter-api:$junitVersjon")
}
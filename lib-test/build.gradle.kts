val ktorVersion = "3.2.1"
val tilgangVersjon = "1.0.95"
val junitVersjon = "5.13.3"

plugins {
    id("behandlingsflyt.conventions")
}

val jacksonVersjon = "2.19.1"

dependencies {
    implementation(project(":behandlingsflyt"))
    implementation(project(":repository"))
    implementation(project(":kontrakt"))
    implementation("no.nav.aap.brev:kontrakt:0.0.130")
    implementation("no.nav.aap.tilgang:api-kontrakt:$tilgangVersjon")
    implementation(libs.httpklient)
    implementation(libs.verdityper)
    implementation(libs.tidslinje)
    implementation(libs.dbconnect)
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    constraints {
        implementation("io.netty:netty-common:4.2.2.Final")
    }
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")

    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersjon")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersjon")

    implementation("ch.qos.logback:logback-classic:1.5.18")

    implementation("com.nimbusds:nimbus-jose-jwt:10.3.1")

    implementation("org.junit.jupiter:junit-jupiter-api:$junitVersjon")
    implementation("org.assertj:assertj-core:3.27.3")
}

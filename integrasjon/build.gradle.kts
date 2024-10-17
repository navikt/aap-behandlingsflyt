val ktorVersion = "3.0.0"
val komponenterVersjon = "1.0.22"

plugins {
    id("behandlingsflyt.conventions")
}

dependencies {
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.0")
 
    implementation("ch.qos.logback:logback-classic:1.5.8")
    implementation("no.nav:ktor-openapi-generator:1.0.46")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.2")
    testImplementation("org.assertj:assertj-core:3.26.3")
}

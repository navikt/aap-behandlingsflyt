plugins {
    id("behandlingsflyt.conventions")
}

val komponenterVersjon = "1.0.79"

dependencies {
    implementation(project(":behandlingsflyt"))

    implementation("ch.qos.logback:logback-classic:1.5.12")
    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbmigrering:$komponenterVersjon")
    implementation("org.flywaydb:flyway-database-postgresql:10.22.0")
    runtimeOnly("org.postgresql:postgresql:42.7.4")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.3")
    testImplementation("org.assertj:assertj-core:3.26.3")
}
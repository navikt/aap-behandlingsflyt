plugins {
    id("behandlingsflyt.conventions")
}

val komponenterVersjon = "1.0.277"
val junitVersjon = "5.13.2"

dependencies {
    implementation(project(":behandlingsflyt"))

    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:verdityper:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbmigrering:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:tidslinje:$komponenterVersjon")
    implementation("org.flywaydb:flyway-database-postgresql:11.10.1")
    implementation("io.getunleash:unleash-client-java:11.0.2")
    runtimeOnly("org.postgresql:postgresql:42.7.7")

    testImplementation(project(":lib-test"))
    testImplementation("no.nav.aap.kelvin:dbtest:$komponenterVersjon")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersjon")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersjon")
    testImplementation("org.assertj:assertj-core:3.27.3")
}

val komponenterVersjon = "1.0.50"

plugins {
    id("behandlingsflyt.conventions")
}

dependencies {
    implementation("no.nav.aap.kelvin:dbmigrering:$komponenterVersjon")
    implementation("org.flywaydb:flyway-database-postgresql:10.18.2")
    runtimeOnly("org.postgresql:postgresql:42.7.4")
}

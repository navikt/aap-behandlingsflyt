val komponenterVersjon = "0.0.91"

plugins {
    id("behandlingsflyt.conventions")
}

dependencies {
    implementation("no.nav.aap.kelvin:dbmigrering:$komponenterVersjon")
    implementation("org.flywaydb:flyway-database-postgresql:10.18.2")
    runtimeOnly("org.postgresql:postgresql:42.7.4")
}

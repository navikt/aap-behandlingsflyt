val komponenterVersjon = "0.0.23"

dependencies {
    implementation("no.nav.aap.kelvin:dbmigrering:$komponenterVersjon")
    implementation("org.flywaydb:flyway-database-postgresql:10.17.2")
    runtimeOnly("org.postgresql:postgresql:42.7.4")
}

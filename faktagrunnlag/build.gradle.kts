val ktorVersion = "2.3.12"
val komponenterVersjon = "0.0.23"

dependencies {
    implementation(project(":sakogbehandling"))
    implementation(project(":verdityper"))
    implementation(project(":tidslinje"))
    implementation(project(":dbflyway"))
    implementation(project(":httpklient"))
    implementation(project(":tilgang"))

    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbmigrering:$komponenterVersjon")
    implementation("no.nav:ktor-openapi-generator:1.0.18")
    implementation("io.ktor:ktor-http-jvm:$ktorVersion")
    implementation("com.zaxxer:HikariCP:5.1.0")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")

    implementation("ch.qos.logback:logback-classic:1.5.7")

    testImplementation("no.nav.aap.kelvin:dbtest:$komponenterVersjon")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.0")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation(project(":lib-test"))

}
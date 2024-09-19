val komponenterVersjon = "0.0.63"

dependencies {
    api(project(":kontrakt"))
    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.17.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.0")
    testImplementation("org.assertj:assertj-core:3.26.3")
}
